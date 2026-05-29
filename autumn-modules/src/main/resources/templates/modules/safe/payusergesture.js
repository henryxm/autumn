$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'safe/payusergesture/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.safe_payusergesture_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.safe_payusergesture_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.safe_payusergesture_column_user_uuid}', name: 'userUuid', index: 'user_uuid', width: 80 }, 
			{ label: '${lang.safe_payusergesture_column_gesture_hash}', name: 'gestureHash', index: 'gesture_hash', width: 80 }, 
			{ label: '${lang.safe_payusergesture_column_salt}', name: 'salt', index: 'salt', width: 80 }, 
			{ label: '${lang.safe_payusergesture_column_status}', name: 'status', index: 'status', width: 80 }, 
			{ label: '${lang.safe_payusergesture_column_fail_count}', name: 'failCount', index: 'fail_count', width: 80 }, 
			{ label: '${lang.safe_payusergesture_column_locked_until}', name: 'lockedUntil', index: 'locked_until', width: 80 }, 
			{ label: '${lang.safe_payusergesture_column_set_time}', name: 'setTime', index: 'set_time', width: 80 }, 
			{ label: '${lang.safe_payusergesture_column_update_time}', name: 'updateTime', index: 'update_time', width: 80 }, 
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
		payUserGesture: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.payUserGesture = {};
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
			var url = vm.payUserGesture.id == null ? "safe/payusergesture/save" : "safe/payusergesture/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.payUserGesture),
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
				    url: baseURL + "safe/payusergesture/delete",
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
			$.get(baseURL + "safe/payusergesture/info/"+id, function(r){
                vm.payUserGesture = r.payUserGesture;
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