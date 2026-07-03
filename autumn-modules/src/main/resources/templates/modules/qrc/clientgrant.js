$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'qrc/clientgrant/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.qrc_clientgrant_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.qrc_clientgrant_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.qrc_clientgrant_column_client_id}', name: 'clientId', index: 'client_id', width: 80 }, 
			{ label: '${lang.qrc_clientgrant_column_enabled}', name: 'enabled', index: 'enabled', width: 80 }, 
			{ label: '${lang.qrc_clientgrant_column_delivery}', name: 'delivery', index: 'delivery', width: 80 }, 
			{ label: '${lang.qrc_clientgrant_column_webhook}', name: 'webhook', index: 'webhook', width: 80 }, 
			{ label: '${lang.qrc_clientgrant_column_secret}', name: 'secret', index: 'secret', width: 80 }, 
			{ label: '${lang.qrc_clientgrant_column_schemes}', name: 'schemes', index: 'schemes', width: 80 }, 
			{ label: '${lang.qrc_clientgrant_column_scopes}', name: 'scopes', index: 'scopes', width: 80 }, 
			{ label: '${lang.qrc_clientgrant_column_consent}', name: 'consent', index: 'consent', width: 80 }, 
			{ label: '${lang.qrc_clientgrant_column_updated}', name: 'updated', index: 'updated', width: 80 }, 
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
		clientGrant: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.clientGrant = {};
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
			var url = vm.clientGrant.id == null ? "qrc/clientgrant/save" : "qrc/clientgrant/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.clientGrant),
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
				    url: baseURL + "qrc/clientgrant/delete",
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
			$.get(baseURL + "qrc/clientgrant/info/"+id, function(r){
                vm.clientGrant = r.clientGrant;
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