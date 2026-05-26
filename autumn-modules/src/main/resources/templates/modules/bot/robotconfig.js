$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'bot/robotconfig/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.bot_robotconfig_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.bot_robotconfig_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.bot_robotconfig_column_max_robots}', name: 'maxRobots', index: 'max_robots', width: 80 }, 
			{ label: '${lang.bot_robotconfig_column_max_tokens}', name: 'maxTokens', index: 'max_tokens', width: 80 }, 
			{ label: '${lang.bot_robotconfig_column_max_hooks}', name: 'maxHooks', index: 'max_hooks', width: 80 }, 
			{ label: '${lang.bot_robotconfig_column_create_time}', name: 'createTime', index: 'create_time', width: 80 }, 
			{ label: '${lang.bot_robotconfig_column_update_time}', name: 'updateTime', index: 'update_time', width: 80 }, 
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
		robotConfig: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.robotConfig = {};
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
			var url = vm.robotConfig.id == null ? "bot/robotconfig/save" : "bot/robotconfig/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.robotConfig),
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
				    url: baseURL + "bot/robotconfig/delete",
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
			$.get(baseURL + "bot/robotconfig/info/"+id, function(r){
                vm.robotConfig = r.robotConfig;
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