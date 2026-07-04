$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'sys/config/list',
        datatype: "json",
        colModel: [
			{ label: '${lang.sys_string_config_name}', name: 'paramKey', sortable: false, width: 80 },
			{ label: '${lang.sys_string_config_value}', name: 'paramValue', width: 80 },
			{ label: '${lang.sys_string_remark}', name: 'remark', width: 300 }
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
            paramKey: null
		},
		showList: true,
		title: null,
		config: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.config = {};
		},
		update: function () {
			var id = getSelectedRow();
			if(id == null){
				return ;
			}
			
			$.get(baseURL + "sys/config/info/"+id, function(r){
                vm.showList = false;
                vm.title = "${lang.sys_string_change}";
                vm.config = r.config;
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
				    url: baseURL + "sys/config/delete",
                    contentType: "application/json",
				    data: JSON.stringify(ids),
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
		saveOrUpdate: function (event) {
			var url = vm.config.id == null ? "sys/config/save" : "sys/config/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.config),
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
		refreshJsonSelected: function () {
			var rowId = $("#jqGrid").jqGrid('getGridParam', 'selrow');
			if (rowId == null) {
				alert('请先选择一条配置');
				return;
			}
			var row = $("#jqGrid").jqGrid('getRowData', rowId);
			vm.doRefreshJson(row.paramKey);
		},
		refreshJsonAll: function () {
			confirm('确定刷新全部 JSON 类型配置的缺失字段？已有字段值将保持不变。', function () {
				vm.doRefreshJson(null);
			});
		},
		doRefreshJson: function (paramKey) {
			var url = baseURL + 'sys/config/refreshJson';
			if (paramKey) {
				url += '?paramKey=' + encodeURIComponent(paramKey);
			}
			$.ajax({
				type: 'POST',
				url: url,
				success: function (r) {
					if (r.code === 0) {
						var lines = [];
						if (r.changed > 0) {
							lines.push('已更新 ' + r.changed + ' 项配置');
						} else {
							lines.push('所有 JSON 配置均已是最新结构');
						}
						if (r.data && r.data.length) {
							r.data.forEach(function (item) {
								if (item.changed && item.addedFields && item.addedFields.length) {
									lines.push(item.paramKey + ' 新增: ' + item.addedFields.join(', '));
								} else if (item.changed && item.fixes && item.fixes.length) {
									lines.push(item.paramKey + ' 修正: ' + item.fixes.join('; '));
								} else if (item.message && item.message.indexOf('失败') >= 0) {
									lines.push(item.paramKey + ': ' + item.message);
								}
							});
						}
						alert(lines.join('\n'), function () {
							vm.reload();
						});
					} else {
						alert(r.msg);
					}
				}
			});
		},
		reload: function (event) {
			vm.showList = true;
			var page = $("#jqGrid").jqGrid('getGridParam','page');
			$("#jqGrid").jqGrid('setGridParam',{ 
                postData:{'paramKey': vm.q.paramKey},
                page:page
            }).trigger("reloadGrid");
		}
	}
});