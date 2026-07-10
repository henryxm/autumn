$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'auth/scopedef/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.auth_scopedef_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.auth_scopedef_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.auth_scopedef_column_code}', name: 'code', index: 'code', width: 80 }, 
			{ label: '${lang.auth_scopedef_column_label}', name: 'label', index: 'label', width: 80 }, 
			{ label: '${lang.auth_scopedef_column_tracks}', name: 'tracks', index: 'tracks', width: 80 }, 
			{ label: '${lang.auth_scopedef_column_fields}', name: 'fields', index: 'fields', width: 80 }, 
			{ label: '${lang.auth_scopedef_column_sensitivity}', name: 'sensitivity', index: 'sensitivity', width: 80 }, 
			{ label: '${lang.auth_scopedef_column_requires}', name: 'requires', index: 'requires', width: 80 }, 
			{ label: '${lang.auth_scopedef_column_enabled}', name: 'enabled', index: 'enabled', width: 80 }, 
			{ label: '${lang.auth_scopedef_column_builtin}', name: 'builtin', index: 'builtin', width: 80 }, 
			{ label: '${lang.auth_scopedef_column_updated}', name: 'updated', index: 'updated', width: 80 }, 
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
		scopeDef: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.scopeDef = {};
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
			var url = vm.scopeDef.id == null ? "auth/scopedef/save" : "auth/scopedef/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.scopeDef),
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
				    url: baseURL + "auth/scopedef/delete",
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
			$.get(baseURL + "auth/scopedef/info/"+id, function(r){
                vm.scopeDef = r.scopeDef;
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