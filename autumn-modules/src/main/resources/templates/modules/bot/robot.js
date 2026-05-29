$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'bot/robot/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.bot_robot_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.bot_robot_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.bot_robot_column_owner}', name: 'owner', index: 'owner', width: 80 }, 
			{ label: '${lang.bot_robot_column_nickname}', name: 'nickname', index: 'nickname', width: 80 }, 
			{ label: '${lang.bot_robot_column_description}', name: 'description', index: 'description', width: 80 }, 
			{ label: '${lang.bot_robot_column_icon}', name: 'icon', index: 'icon', width: 80 }, 
			{ label: '${lang.bot_robot_column_hash}', name: 'hash', index: 'hash', width: 80 }, 
			{ label: '${lang.bot_robot_column_status}', name: 'status', index: 'status', width: 80 }, 
			{ label: '${lang.bot_robot_column_access}', name: 'access', index: 'access', width: 80 }, 
			{ label: '${lang.bot_robot_column_black}', name: 'black', index: 'black', width: 80 }, 
			{ label: '${lang.bot_robot_column_scopes}', name: 'scopes', index: 'scopes', width: 80 }, 
			{ label: '${lang.bot_robot_column_create_time}', name: 'createTime', index: 'create_time', width: 80 }, 
			{ label: '${lang.bot_robot_column_update_time}', name: 'updateTime', index: 'update_time', width: 80 }, 
			{ label: '${lang.bot_robot_column_delete_time}', name: 'deleteTime', index: 'delete_time', width: 80 }, 
			{ label: '${lang.bot_robot_column_destroy_time}', name: 'destroyTime', index: 'destroy_time', width: 80 }, 
			{ label: '${lang.bot_robot_column_last_used_time}', name: 'lastUsedTime', index: 'last_used_time', width: 80 }, 
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
		robot: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.robot = {};
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
			var url = vm.robot.id == null ? "bot/robot/save" : "bot/robot/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.robot),
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
				    url: baseURL + "bot/robot/delete",
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
			$.get(baseURL + "bot/robot/info/"+id, function(r){
                vm.robot = r.robot;
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