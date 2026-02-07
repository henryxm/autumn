$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'db/databasebackupupload/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.db_databasebackupupload_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.db_databasebackupupload_column_original_filename}', name: 'originalFilename', index: 'original_filename', width: 80 }, 
			{ label: '${lang.db_databasebackupupload_column_filename}', name: 'filename', index: 'filename', width: 80 }, 
			{ label: '${lang.db_databasebackupupload_column_filepath}', name: 'filepath', index: 'filepath', width: 80 }, 
			{ label: '${lang.db_databasebackupupload_column_filesize}', name: 'filesize', index: 'filesize', width: 80 }, 
			{ label: '${lang.db_databasebackupupload_column_database}', name: 'database', index: 'database', width: 80 }, 
			{ label: '${lang.db_databasebackupupload_column_remark}', name: 'remark', index: 'remark', width: 80 }, 
			{ label: '${lang.db_databasebackupupload_column_status}', name: 'status', index: 'status', width: 80 }, 
			{ label: '${lang.db_databasebackupupload_column_error}', name: 'error', index: 'error', width: 80 }, 
			{ label: '${lang.db_databasebackupupload_column_restore_duration}', name: 'restoreDuration', index: 'restore_duration', width: 80 }, 
			{ label: '${lang.db_databasebackupupload_column_restore_time}', name: 'restoreTime', index: 'restore_time', width: 80 }, 
			{ label: '${lang.db_databasebackupupload_column_create_time}', name: 'createTime', index: 'create_time', width: 80 }, 
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
		databaseBackupUpload: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.databaseBackupUpload = {};
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
			var url = vm.databaseBackupUpload.id == null ? "db/databasebackupupload/save" : "db/databasebackupupload/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.databaseBackupUpload),
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
				    url: baseURL + "db/databasebackupupload/delete",
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
			$.get(baseURL + "db/databasebackupupload/info/"+id, function(r){
                vm.databaseBackupUpload = r.databaseBackupUpload;
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