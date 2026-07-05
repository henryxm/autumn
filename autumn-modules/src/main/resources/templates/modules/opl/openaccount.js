$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'opl/openaccount/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.opl_openaccount_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.opl_openaccount_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.opl_openaccount_column_user}', name: 'user', index: 'user', width: 80 }, 
			{ label: '${lang.opl_openaccount_column_name}', name: 'name', index: 'name', width: 80 }, 
			{ label: '${lang.opl_openaccount_column_status}', name: 'status', index: 'status', width: 80 }, 
			{ label: '${lang.opl_openaccount_column_create}', name: 'create', index: 'create', width: 80 }, 
			{ label: '${lang.opl_openaccount_column_update}', name: 'update', index: 'update', width: 80 }, 
        ],
		viewrecords: true,
        height: 385,
        rowNum: 10,
		rowList : [10,30,50],
        rownumbers: true, 
        rownumWidth: 25, 
        autowidth:true,
        multiselect: true,
        pager: "#jqGridPager",
        jsonReader : {
            root: "page.list",
            page: "page.currPage",
            total: "page.totalPage",
            records: "page.totalCount"
        },
        prmNames : {
            page:"page", 
            rows:"limit", 
            order: "order"
        },
        gridComplete:function(){
        	//隐藏grid底部滚动条
        	$("#jqGrid").closest(".ui-jqgrid-bdiv").css({ "overflow-x" : "hidden" }); 
        }
    });
});

var vm = new Vue({
	el:'#rrapp',
	data:{
		showList: true,
		title: null,
		openAccount: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.openAccount = {};
		},
		update: function (event) {
			var id = getSelectedRow();
			if(id == null){
				return ;
			}
			vm.showList = false;
            vm.title = "${lang.sys_string_change}";
            
            vm.getInfo(id)
		},
		saveOrUpdate: function (event) {
			var url = vm.openAccount.id == null ? "opl/openaccount/save" : "opl/openaccount/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.openAccount),
			    success: function(r){
			    	if(r.code === 0){
						alert('${lang.sys_string_successful}', function(index){
							vm.reload();
						});
					}else{
						alert(r.msg);
					}
				}
			});
		},
		del: function (event) {
			var ids = getSelectedRows();
			if(ids == null){
				return ;
			}
			confirm('${lang.sys_string_are_sure_to_delete}？', function(){
				$.ajax({
					type: "POST",
				    url: baseURL + "opl/openaccount/delete",
                    contentType: "application/json",
				    data: JSON.stringify(ids),
				    success: function(r){
						if(r.code == 0){
							alert('${lang.sys_string_successful}', function(index){
								$("#jqGrid").trigger("reloadGrid");
							});
						}else{
							alert(r.msg);
						}
					}
				});
			});
		},
		getInfo: function(id){
			$.get(baseURL + "opl/openaccount/info/"+id, function(r){
                vm.openAccount = r.openAccount;
            });
		},
		reload: function (event) {
			vm.showList = true;
			var page = $("#jqGrid").jqGrid('getGridParam','page');
			$("#jqGrid").jqGrid('setGridParam',{ 
                page:page
            }).trigger("reloadGrid");
		}
	}
});