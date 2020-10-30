$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'job/schedulejob/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.job_schedulejob_column_job_id}', name: 'jobId', width: 60, key: true },
			{ label: '${lang.job_schedulejob_column_bean_name}', name: 'beanName', width: 100 },
			{ label: '${lang.job_schedulejob_column_method_name}', name: 'methodName', width: 100 },
			{ label: '${lang.job_schedulejob_column_params}', name: 'params', width: 100 },
			{ label: '${lang.job_schedulejob_column_cron_expression} ', name: 'cronExpression', width: 100 },
			{ label: '${lang.job_schedulejob_column_mode}', name: 'mode', index: 'mode', width: 80 },
			{ label: '${lang.job_schedulejob_column_remark} ', name: 'remark', width: 100 },
			{ label: '${lang.job_schedulejob_column_status}', name: 'status', width: 60, formatter: function(value, options, row){
				return value === 0 ? 
					'<span class="label label-success">${lang.sys_string_normal}</span>' :
					'<span class="label label-danger">${lang.sys_string_suspend}</span>';
			}}
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
		q:{
			beanName: null
		},
		showList: true,
		title: null,
		scheduleJob: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.scheduleJob = {};
		},
		update: function () {
			var jobId = getSelectedRow();
			if(jobId == null){
				return ;
			}
			
			$.get(baseURL + "job/schedulejob/info/"+jobId, function(r){
				vm.showList = false;
                vm.title = "${lang.sys_string_change}";
				vm.scheduleJob = r.scheduleJob;
			});
		},
		saveOrUpdate: function (event) {
			var url = vm.scheduleJob.jobId == null ? "job/schedulejob/save" : "job/schedulejob/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.scheduleJob),
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
			var jobIds = getSelectedRows();
			if(jobIds == null){
				return ;
			}
			
			confirm('${lang.sys_string_are_sure_to_delete}？', function(){
				$.ajax({
					type: "POST",
				    url: baseURL + "job/schedulejob/delete",
                    contentType: "application/json",
				    data: JSON.stringify(jobIds),
				    success: function(r){
						if(r.code == 0){
							alert('${lang.sys_string_successful}', function(index){
								vm.reload();
							});
						}else{
							alert(r.msg);
						}
					}
				});
			});
		},
		pause: function (event) {
			var jobIds = getSelectedRows();
			if(jobIds == null){
				return ;
			}
			
			confirm('${lang.sys_string_are_sure_to_pause}？', function(){
				$.ajax({
					type: "POST",
				    url: baseURL + "job/schedulejob/pause",
                    contentType: "application/json",
				    data: JSON.stringify(jobIds),
				    success: function(r){
						if(r.code == 0){
							alert('${lang.sys_string_successful}', function(index){
								vm.reload();
							});
						}else{
							alert(r.msg);
						}
					}
				});
			});
		},
		resume: function (event) {
			var jobIds = getSelectedRows();
			if(jobIds == null){
				return ;
			}
			
			confirm('${lang.sys_string_are_sure_to_resume}？', function(){
				$.ajax({
					type: "POST",
				    url: baseURL + "job/schedulejob/resume",
                    contentType: "application/json",
				    data: JSON.stringify(jobIds),
				    success: function(r){
						if(r.code == 0){
							alert('${lang.sys_string_successful}', function(index){
								vm.reload();
							});
						}else{
							alert(r.msg);
						}
					}
				});
			});
		},
		runOnce: function (event) {
			var jobIds = getSelectedRows();
			if(jobIds == null){
				return ;
			}
			
			confirm('${lang.sys_string_are_sure_to_execute}？', function(){
				$.ajax({
					type: "POST",
				    url: baseURL + "job/schedulejob/run",
                    contentType: "application/json",
				    data: JSON.stringify(jobIds),
				    success: function(r){
						if(r.code == 0){
							alert('${lang.sys_string_successful}', function(index){
								vm.reload();
							});
						}else{
							alert(r.msg);
						}
					}
				});
			});
		},
		reload: function (event) {
			vm.showList = true;
			var page = $("#jqGrid").jqGrid('getGridParam','page');
			$("#jqGrid").jqGrid('setGridParam',{ 
                postData:{'beanName': vm.q.beanName},
                page:page 
            }).trigger("reloadGrid");
		}
	}
});