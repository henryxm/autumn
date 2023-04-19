$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'oauth/clientdetails/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.oauth_clientdetails_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.oauth_clientdetails_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_resource_ids}', name: 'resourceIds', index: 'resource_ids', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_scope}', name: 'scope', index: 'scope', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_grant_types}', name: 'grantTypes', index: 'grant_types', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_roles}', name: 'roles', index: 'roles', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_trusted}', name: 'trusted', index: 'trusted', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_archived}', name: 'archived', index: 'archived', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_client_id}', name: 'clientId', index: 'client_id', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_client_secret}', name: 'clientSecret', index: 'client_secret', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_client_name}', name: 'clientName', index: 'client_name', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_client_uri}', name: 'clientUri', index: 'client_uri', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_client_icon_uri}', name: 'clientIconUri', index: 'client_icon_uri', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_redirect_uri}', name: 'redirectUri', index: 'redirect_uri', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_client_type}', name: 'clientType', index: 'client_type', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_description}', name: 'description', index: 'description', width: 80 }, 
			{ label: '${lang.oauth_clientdetails_column_create_time}', name: 'createTime', index: 'create_time', width: 80 }, 
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
		clientDetails: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.clientDetails = {};
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
			var url = vm.clientDetails.id == null ? "oauth/clientdetails/save" : "oauth/clientdetails/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.clientDetails),
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
				    url: baseURL + "oauth/clientdetails/delete",
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
			$.get(baseURL + "oauth/clientdetails/info/"+id, function(r){
                vm.clientDetails = r.clientDetails;
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