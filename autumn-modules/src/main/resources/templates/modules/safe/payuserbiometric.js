$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'safe/payuserbiometric/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.safe_payuserbiometric_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.safe_payuserbiometric_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.safe_payuserbiometric_column_user_uuid}', name: 'userUuid', index: 'user_uuid', width: 80 }, 
			{ label: '${lang.safe_payuserbiometric_column_device_id}', name: 'deviceId', index: 'device_id', width: 80 }, 
			{ label: '${lang.safe_payuserbiometric_column_platform}', name: 'platform', index: 'platform', width: 80 }, 
			{ label: '${lang.safe_payuserbiometric_column_credential_id}', name: 'credentialId', index: 'credential_id', width: 80 }, 
			{ label: '${lang.safe_payuserbiometric_column_public_key}', name: 'publicKey', index: 'public_key', width: 80 }, 
			{ label: '${lang.safe_payuserbiometric_column_status}', name: 'status', index: 'status', width: 80 }, 
			{ label: '${lang.safe_payuserbiometric_column_last_used_time}', name: 'lastUsedTime', index: 'last_used_time', width: 80 }, 
			{ label: '${lang.safe_payuserbiometric_column_create_time}', name: 'createTime', index: 'create_time', width: 80 }, 
			{ label: '${lang.safe_payuserbiometric_column_update_time}', name: 'updateTime', index: 'update_time', width: 80 }, 
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
		payUserBiometric: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.payUserBiometric = {};
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
			var url = vm.payUserBiometric.id == null ? "safe/payuserbiometric/save" : "safe/payuserbiometric/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.payUserBiometric),
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
				    url: baseURL + "safe/payuserbiometric/delete",
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
			$.get(baseURL + "safe/payuserbiometric/info/"+id, function(r){
                vm.payUserBiometric = r.payUserBiometric;
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