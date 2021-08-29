$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'oauth/tokenstore/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.oauth_tokenstore_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.oauth_tokenstore_column_user_uuid}', name: 'userUuid', index: 'user_uuid', width: 80 }, 
			{ label: '${lang.oauth_tokenstore_column_auth_code}', name: 'authCode', index: 'auth_code', width: 80 }, 
			{ label: '${lang.oauth_tokenstore_column_access_token}', name: 'accessToken', index: 'access_token', width: 80 }, 
			{ label: '${lang.oauth_tokenstore_column_access_token_expired_in}', name: 'accessTokenExpiredIn', index: 'access_token_expired_in', width: 80 }, 
			{ label: '${lang.oauth_tokenstore_column_refresh_token}', name: 'refreshToken', index: 'refresh_token', width: 80 }, 
			{ label: '${lang.oauth_tokenstore_column_refresh_token_expired_in}', name: 'refreshTokenExpiredIn', index: 'refresh_token_expired_in', width: 80 }, 
			{ label: '${lang.oauth_tokenstore_column_create_time}', name: 'createTime', index: 'create_time', width: 80 }, 
			{ label: '${lang.oauth_tokenstore_column_update_time}', name: 'updateTime', index: 'update_time', width: 80 }, 
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
		tokenStore: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.tokenStore = {};
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
			var url = vm.tokenStore.id == null ? "oauth/tokenstore/save" : "oauth/tokenstore/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.tokenStore),
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
				    url: baseURL + "oauth/tokenstore/delete",
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
			$.get(baseURL + "oauth/tokenstore/info/"+id, function(r){
                vm.tokenStore = r.tokenStore;
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