$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'safe/paycredentiallog/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.safe_paycredentiallog_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.safe_paycredentiallog_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.safe_paycredentiallog_column_user_uuid}', name: 'userUuid', index: 'user_uuid', width: 80 }, 
			{ label: '${lang.safe_paycredentiallog_column_action}', name: 'action', index: 'action', width: 80 }, 
			{ label: '${lang.safe_paycredentiallog_column_method}', name: 'method', index: 'method', width: 80 }, 
			{ label: '${lang.safe_paycredentiallog_column_success}', name: 'success', index: 'success', width: 80 }, 
			{ label: '${lang.safe_paycredentiallog_column_ip}', name: 'ip', index: 'ip', width: 80 }, 
			{ label: '${lang.safe_paycredentiallog_column_user_agent}', name: 'userAgent', index: 'user_agent', width: 80 }, 
			{ label: '${lang.safe_paycredentiallog_column_remark}', name: 'remark', index: 'remark', width: 80 }, 
			{ label: '${lang.safe_paycredentiallog_column_create_time}', name: 'createTime', index: 'create_time', width: 80 }, 
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
		payCredentialLog: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.payCredentialLog = {};
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
			var url = vm.payCredentialLog.id == null ? "safe/paycredentiallog/save" : "safe/paycredentiallog/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.payCredentialLog),
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
				    url: baseURL + "safe/paycredentiallog/delete",
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
			$.get(baseURL + "safe/paycredentiallog/info/"+id, function(r){
                vm.payCredentialLog = r.payCredentialLog;
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