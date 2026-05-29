$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'safe/paygateattempt/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.safe_paygateattempt_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.safe_paygateattempt_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.safe_paygateattempt_column_user_uuid}', name: 'userUuid', index: 'user_uuid', width: 80 }, 
			{ label: '${lang.safe_paygateattempt_column_amount_cent}', name: 'amountCent', index: 'amount_cent', width: 80 }, 
			{ label: '${lang.safe_paygateattempt_column_currency}', name: 'currency', index: 'currency', width: 80 }, 
			{ label: '${lang.safe_paygateattempt_column_order_id}', name: 'orderId', index: 'order_id', width: 80 }, 
			{ label: '${lang.safe_paygateattempt_column_merchant_id}', name: 'merchantId', index: 'merchant_id', width: 80 }, 
			{ label: '${lang.safe_paygateattempt_column_reason}', name: 'reason', index: 'reason', width: 80 }, 
			{ label: '${lang.safe_paygateattempt_column_device_id}', name: 'deviceId', index: 'device_id', width: 80 }, 
			{ label: '${lang.safe_paygateattempt_column_ip}', name: 'ip', index: 'ip', width: 80 }, 
			{ label: '${lang.safe_paygateattempt_column_location}', name: 'location', index: 'location', width: 80 }, 
			{ label: '${lang.safe_paygateattempt_column_auth_mode}', name: 'authMode', index: 'auth_mode', width: 80 }, 
			{ label: '${lang.safe_paygateattempt_column_authorized}', name: 'authorized', index: 'authorized', width: 80 }, 
			{ label: '${lang.safe_paygateattempt_column_detail_json}', name: 'detailJson', index: 'detail_json', width: 80 }, 
			{ label: '${lang.safe_paygateattempt_column_create_time}', name: 'createTime', index: 'create_time', width: 80 }, 
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
		payGateAttempt: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.payGateAttempt = {};
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
			var url = vm.payGateAttempt.id == null ? "safe/paygateattempt/save" : "safe/paygateattempt/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.payGateAttempt),
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
				    url: baseURL + "safe/paygateattempt/delete",
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
			$.get(baseURL + "safe/paygateattempt/info/"+id, function(r){
                vm.payGateAttempt = r.payGateAttempt;
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