$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'spm/visitlog/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.spm_visitlog_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.spm_visitlog_column_site_id}', name: 'siteId', index: 'site_id', width: 80 }, 
			{ label: '${lang.spm_visitlog_column_page_id}', name: 'pageId', index: 'page_id', width: 80 }, 
			{ label: '${lang.spm_visitlog_column_channel_id}', name: 'channelId', index: 'channel_id', width: 80 }, 
			{ label: '${lang.spm_visitlog_column_product_id}', name: 'productId', index: 'product_id', width: 80 }, 
			{ label: '${lang.spm_visitlog_column_unique_visitor}', name: 'uniqueVisitor', index: 'unique_visitor', width: 80 }, 
			{ label: '${lang.spm_visitlog_column_page_view}', name: 'pageView', index: 'page_view', width: 80 }, 
			{ label: '${lang.spm_visitlog_column_day_string}', name: 'dayString', index: 'day_string', width: 80 }, 
			{ label: '${lang.spm_visitlog_column_create_time}', name: 'createTime', index: 'create_time', width: 80 }, 
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
		visitLog: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.visitLog = {};
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
			var url = vm.visitLog.id == null ? "spm/visitlog/save" : "spm/visitlog/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.visitLog),
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
				    url: baseURL + "spm/visitlog/delete",
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
			$.get(baseURL + "spm/visitlog/info/"+id, function(r){
                vm.visitLog = r.visitLog;
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