$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'opc/connectapp/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.opc_connectapp_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.opc_connectapp_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.opc_connectapp_column_user}', name: 'user', index: 'user', width: 80 }, 
			{ label: '${lang.opc_connectapp_column_app_id}', name: 'appId', index: 'app_id', width: 80 }, 
			{ label: '${lang.opc_connectapp_column_app_secret}', name: 'appSecret', index: 'app_secret', width: 80 }, 
			{ label: '${lang.opc_connectapp_column_platform_base_url}', name: 'platformBaseUrl', index: 'platform_base_url', width: 80 }, 
			{ label: '${lang.opc_connectapp_column_redirect_uri}', name: 'redirectUri', index: 'redirect_uri', width: 80 }, 
			{ label: '${lang.opc_connectapp_column_authorize_uri}', name: 'authorizeUri', index: 'authorize_uri', width: 80 }, 
			{ label: '${lang.opc_connectapp_column_token_uri}', name: 'tokenUri', index: 'token_uri', width: 80 }, 
			{ label: '${lang.opc_connectapp_column_user_info_uri}', name: 'userInfoUri', index: 'user_info_uri', width: 80 }, 
			{ label: '${lang.opc_connectapp_column_name}', name: 'name', index: 'name', width: 80 }, 
			{ label: '${lang.opc_connectapp_column_scope}', name: 'scope', index: 'scope', width: 80 }, 
			{ label: '${lang.opc_connectapp_column_status}', name: 'status', index: 'status', width: 80 }, 
			{ label: '${lang.opc_connectapp_column_create}', name: 'create', index: 'create', width: 80 }, 
			{ label: '${lang.opc_connectapp_column_update}', name: 'update', index: 'update', width: 80 }, 
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
		connectApp: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.connectApp = {};
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
			var url = vm.connectApp.id == null ? "opc/connectapp/save" : "opc/connectapp/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.connectApp),
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
				    url: baseURL + "opc/connectapp/delete",
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
			$.get(baseURL + "opc/connectapp/info/"+id, function(r){
                vm.connectApp = r.connectApp;
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