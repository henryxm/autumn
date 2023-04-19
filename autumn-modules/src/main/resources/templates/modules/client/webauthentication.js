$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'client/webauthentication/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.client_webauthentication_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.client_webauthentication_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.client_webauthentication_column_name}', name: 'name', index: 'name', width: 80 }, 
			{ label: '${lang.client_webauthentication_column_client_id}', name: 'clientId', index: 'client_id', width: 80 }, 
			{ label: '${lang.client_webauthentication_column_client_secret}', name: 'clientSecret', index: 'client_secret', width: 80 }, 
			{ label: '${lang.client_webauthentication_column_redirect_uri}', name: 'redirectUri', index: 'redirect_uri', width: 80 }, 
			{ label: '${lang.client_webauthentication_column_authorize_uri}', name: 'authorizeUri', index: 'authorize_uri', width: 80 }, 
			{ label: '${lang.client_webauthentication_column_access_token_uri}', name: 'accessTokenUri', index: 'access_token_uri', width: 80 }, 
			{ label: '${lang.client_webauthentication_column_user_info_uri}', name: 'userInfoUri', index: 'user_info_uri', width: 80 }, 
			{ label: '${lang.client_webauthentication_column_scope}', name: 'scope', index: 'scope', width: 80 }, 
			{ label: '${lang.client_webauthentication_column_state}', name: 'state', index: 'state', width: 80 }, 
			{ label: '${lang.client_webauthentication_column_client_type}', name: 'clientType', index: 'client_type', width: 80 }, 
			{ label: '${lang.client_webauthentication_column_description}', name: 'description', index: 'description', width: 80 }, 
			{ label: '${lang.client_webauthentication_column_create_time}', name: 'createTime', index: 'create_time', width: 80 }, 
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
		webAuthentication: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.webAuthentication = {};
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
			var url = vm.webAuthentication.id == null ? "client/webauthentication/save" : "client/webauthentication/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.webAuthentication),
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
				    url: baseURL + "client/webauthentication/delete",
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
			$.get(baseURL + "client/webauthentication/info/"+id, function(r){
                vm.webAuthentication = r.webAuthentication;
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