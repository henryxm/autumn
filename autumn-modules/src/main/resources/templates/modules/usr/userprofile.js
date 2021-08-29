$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'usr/userprofile/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.usr_userprofile_column_uuid}', name: 'uuid', index: 'uuid', width: 50, key: true },
			{ label: '${lang.usr_userprofile_column_open_id}', name: 'openId', index: 'open_id', width: 80 }, 
			{ label: '${lang.usr_userprofile_column_union_id}', name: 'unionId', index: 'union_id', width: 80 }, 
			{ label: '${lang.usr_userprofile_column_icon}', name: 'icon', index: 'icon', width: 80 }, 
			{ label: '${lang.usr_userprofile_column_username}', name: 'username', index: 'username', width: 80 }, 
			{ label: '${lang.usr_userprofile_column_nickname}', name: 'nickname', index: 'nickname', width: 80 }, 
			{ label: '${lang.usr_userprofile_column_mobile}', name: 'mobile', index: 'mobile', width: 80 }, 
			{ label: '${lang.usr_userprofile_column_password}', name: 'password', index: 'password', width: 80 }, 
			{ label: '${lang.usr_userprofile_column_create_time}', name: 'createTime', index: 'create_time', width: 80 }, 
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
		userProfile: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.userProfile = {};
		},
		update: function (event) {
			var uuid = getSelectedRow();
			if(uuid == null){
				return ;
			}
			vm.showList = false;
            vm.title = "${lang.sys_string_change}";
            
            vm.getInfo(uuid)
		},
		saveOrUpdate: function (event) {
			var url = vm.userProfile.uuid == null ? "usr/userprofile/save" : "usr/userprofile/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.userProfile),
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
			var uuids = getSelectedRows();
			if(uuids == null){
				return ;
			}
			confirm('${lang.sys_string_are_sure_to_delete}？', function(){
				$.ajax({
					type: "POST",
				    url: baseURL + "usr/userprofile/delete",
                    contentType: "application/json",
				    data: JSON.stringify(uuids),
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
		getInfo: function(uuid){
			$.get(baseURL + "usr/userprofile/info/"+uuid, function(r){
                vm.userProfile = r.userProfile;
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