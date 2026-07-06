$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'opl/opencode/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.opl_opencode_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.opl_opencode_column_code}', name: 'code', index: 'code', width: 80 }, 
			{ label: '${lang.opl_opencode_column_app_id}', name: 'appId', index: 'app_id', width: 80 }, 
			{ label: '${lang.opl_opencode_column_user}', name: 'user', index: 'user', width: 80 }, 
			{ label: '${lang.opl_opencode_column_redirect_uri}', name: 'redirectUri', index: 'redirect_uri', width: 80 }, 
			{ label: '${lang.opl_opencode_column_expire}', name: 'expire', index: 'expire', width: 80 }, 
			{ label: '${lang.opl_opencode_column_create}', name: 'create', index: 'create', width: 80 }, 
			{ label: '${lang.opl_opencode_column_code_challenge}', name: 'codeChallenge', index: 'code_challenge', width: 80 }, 
			{ label: '${lang.opl_opencode_column_code_challenge_method}', name: 'codeChallengeMethod', index: 'code_challenge_method', width: 80 }, 
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
		openCode: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.openCode = {};
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
			var url = vm.openCode.id == null ? "opl/opencode/save" : "opl/opencode/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.openCode),
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
				    url: baseURL + "opl/opencode/delete",
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
			$.get(baseURL + "opl/opencode/info/"+id, function(r){
                vm.openCode = r.openCode;
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