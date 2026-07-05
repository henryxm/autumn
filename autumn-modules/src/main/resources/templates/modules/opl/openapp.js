$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'opl/openapp/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.opl_openapp_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.opl_openapp_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.opl_openapp_column_account}', name: 'account', index: 'account', width: 80 }, 
			{ label: '${lang.opl_openapp_column_app_id}', name: 'appId', index: 'app_id', width: 80 }, 
			{ label: '${lang.opl_openapp_column_app_secret_hash}', name: 'appSecretHash', index: 'app_secret_hash', width: 80 }, 
			{ label: '${lang.opl_openapp_column_app_secret_salt}', name: 'appSecretSalt', index: 'app_secret_salt', width: 80 }, 
			{ label: '${lang.opl_openapp_column_name}', name: 'name', index: 'name', width: 80 }, 
			{ label: '${lang.opl_openapp_column_app_type}', name: 'appType', index: 'app_type', width: 80 }, 
			{ label: '${lang.opl_openapp_column_redirect_uri}', name: 'redirectUri', index: 'redirect_uri', width: 80 }, 
			{ label: '${lang.opl_openapp_column_scope}', name: 'scope', index: 'scope', width: 80 }, 
			{ label: '${lang.opl_openapp_column_status}', name: 'status', index: 'status', width: 80 }, 
			{ label: '${lang.opl_openapp_column_create}', name: 'create', index: 'create', width: 80 }, 
			{ label: '${lang.opl_openapp_column_update}', name: 'update', index: 'update', width: 80 }, 
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
		openApp: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.openApp = {};
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
			var url = vm.openApp.id == null ? "opl/openapp/save" : "opl/openapp/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.openApp),
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
				    url: baseURL + "opl/openapp/delete",
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
			$.get(baseURL + "opl/openapp/info/"+id, function(r){
                vm.openApp = r.openApp;
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