$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'bot/guest/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.bot_guest_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.bot_guest_column_uuid}', name: 'uuid', index: 'uuid', width: 80 }, 
			{ label: '${lang.bot_guest_column_guest_key}', name: 'guestKey', index: 'guest_key', width: 80 }, 
			{ label: '${lang.bot_guest_column_nickname}', name: 'nickname', index: 'nickname', width: 80 }, 
			{ label: '${lang.bot_guest_column_icon}', name: 'icon', index: 'icon', width: 80 }, 
			{ label: '${lang.bot_guest_column_status}', name: 'status', index: 'status', width: 80 }, 
			{ label: '${lang.bot_guest_column_expire_time}', name: 'expireTime', index: 'expire_time', width: 80 }, 
			{ label: '${lang.bot_guest_column_create_time}', name: 'createTime', index: 'create_time', width: 80 }, 
			{ label: '${lang.bot_guest_column_update_time}', name: 'updateTime', index: 'update_time', width: 80 }, 
			{ label: '${lang.bot_guest_column_last_seen_time}', name: 'lastSeenTime', index: 'last_seen_time', width: 80 }, 
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
		guest: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.guest = {};
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
			var url = vm.guest.id == null ? "bot/guest/save" : "bot/guest/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.guest),
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
				    url: baseURL + "bot/guest/delete",
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
			$.get(baseURL + "bot/guest/info/"+id, function(r){
                vm.guest = r.guest;
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