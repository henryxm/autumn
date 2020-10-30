$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'job/schedulejoblog/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.job_schedulejoblog_column_log_id}', name: 'logId', index: 'log_id', width: 50, key: true },
			{ label: '${lang.job_schedulejoblog_column_job_id}', name: 'jobId', index: 'job_id', width: 50 },
			{ label: '${lang.job_schedulejoblog_column_bean_name}', name: 'beanName', index: 'bean_name', width: 60 },
			{ label: '${lang.job_schedulejoblog_column_method_name}', name: 'methodName', index: 'method_name', width: 60 },
			{ label: '${lang.job_schedulejoblog_column_params}', name: 'params', index: 'params', width: 80 }, 			
			{ label: '${lang.job_schedulejoblog_column_status}', name: 'status', index: 'status', width: 50, formatter: function(value, options, row){
					return value === 0 ? '<span class="label label-success">${lang.sys_string_success}</span>' :
						'<span class="label label-danger pointer" onclick="vm.showError('+row.logId+')">${lang.sys_string_fail}</span>';
			}},
			{ label: '${lang.job_schedulejoblog_column_error}', name: 'error', index: 'error', width: 80 }, 			
			{ label: '${lang.job_schedulejoblog_column_times}', name: 'times', index: 'times', width: 80 }, 			
			{ label: '${lang.job_schedulejoblog_column_create_time}', name: 'createTime', index: 'create_time', width: 80 }, 			
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
		scheduleJobLog: {},
		jobId: null,
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.scheduleJobLog = {};
		},
		update: function (event) {
			var logId = getSelectedRow();
			if(logId == null){
				return ;
			}
			vm.showList = false;
            vm.title = "${lang.sys_string_change}";
            
            vm.getInfo(logId)
		},
		saveOrUpdate: function (event) {
			var url = vm.scheduleJobLog.logId == null ? "job/schedulejoblog/save" : "job/schedulejoblog/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.scheduleJobLog),
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
			var logIds = getSelectedRows();
			if(logIds == null){
				return ;
			}
			
			confirm('${lang.sys_string_are_sure_to_delete}？', function(){
				$.ajax({
					type: "POST",
				    url: baseURL + "job/schedulejoblog/delete",
                    contentType: "application/json",
				    data: JSON.stringify(logIds),
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
		getInfo: function(logId){
			$.get(baseURL + "job/schedulejoblog/info/"+logId, function(r){
                vm.scheduleJobLog = r.scheduleJobLog;
            });
		},
		reload: function (event) {
			vm.showList = true;
			var page = $("#jqGrid").jqGrid('getGridParam','page');
			$("#jqGrid").jqGrid('setGridParam',{ 
                page:page
            }).trigger("reloadGrid");
		},
		query: function () {
			$("#jqGrid").jqGrid('setGridParam',{
				postData:{'jobId': vm.jobId},
				page:1
			}).trigger("reloadGrid");
		},
		showError: function(logId) {
			$.get(baseURL + "job/schedulejoblog/info/"+logId, function(r){
				parent.layer.open({
					title:'${lang.sys_string_fail_message}',
					closeBtn:0,
					content: r.log.error
				});
			});
		},
		back: function (event) {
			history.go(-1);
		}
	}
});