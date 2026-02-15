$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'usr/useropen/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.usr_useropen_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.usr_useropen_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 

			{ label: '${lang.usr_useropen_column_platform}', name: 'platform', index: 'platform', width: 80 }, 

			{ label: '${lang.usr_useropen_column_appid}', name: 'appid', index: 'appid', width: 80 }, 

			{ label: '${lang.usr_useropen_column_openid}', name: 'openid', index: 'openid', width: 80 }, 

			{ label: '${lang.usr_useropen_column_unionid}', name: 'unionid', index: 'unionid', width: 80 }, 

			{ label: '${lang.usr_useropen_column_deleted}', name: 'deleted', index: 'deleted', width: 80 }, 

			{ label: '${lang.usr_useropen_column_create}', name: 'create', index: 'create', width: 80 }, 

			{ label: '${lang.usr_useropen_column_update}', name: 'update', index: 'update', width: 80 }, 

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
		userOpen: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.userOpen = {};
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
			var url = vm.userOpen.id == null ? "usr/useropen/save" : "usr/useropen/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.userOpen),
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
				    url: baseURL + "usr/useropen/delete",
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
			$.get(baseURL + "usr/useropen/info/"+id, function(r){
                vm.userOpen = r.userOpen;
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