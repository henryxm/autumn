$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'bot/robottoken/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.bot_robottoken_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.bot_robottoken_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.bot_robottoken_column_robot}', name: 'robot', index: 'robot', width: 80 }, 
			{ label: '${lang.bot_robottoken_column_token}', name: 'token', index: 'token', width: 80 }, 
			{ label: '${lang.bot_robottoken_column_token_prefix}', name: 'tokenPrefix', index: 'token_prefix', width: 80 }, 
			{ label: '${lang.bot_robottoken_column_status}', name: 'status', index: 'status', width: 80 }, 
			{ label: '${lang.bot_robottoken_column_expire_time}', name: 'expireTime', index: 'expire_time', width: 80 }, 
			{ label: '${lang.bot_robottoken_column_update_time}', name: 'updateTime', index: 'update_time', width: 80 }, 
			{ label: '${lang.bot_robottoken_column_last_used_time}', name: 'lastUsedTime', index: 'last_used_time', width: 80 }, 
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
		robotToken: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.robotToken = {};
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
			var url = vm.robotToken.id == null ? "bot/robottoken/save" : "bot/robottoken/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.robotToken),
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
				    url: baseURL + "bot/robottoken/delete",
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
			$.get(baseURL + "bot/robottoken/info/"+id, function(r){
                vm.robotToken = r.robotToken;
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