$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'spm/superpositionmodel/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.spm_superpositionmodel_column_id}', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.spm_superpositionmodel_column_site_id}', name: 'siteId', index: 'site_id', width: 50 },
			{ label: '${lang.spm_superpositionmodel_column_page_id}', name: 'pageId', index: 'page_id', width: 50 },
			{ label: '${lang.spm_superpositionmodel_column_channel_id}', name: 'channelId', index: 'channel_id', width: 50 },
			{ label: '${lang.spm_superpositionmodel_column_product_id}', name: 'productId', index: 'product_id', width: 50 },
			{ label: '${lang.spm_superpositionmodel_column_resource_id}', name: 'resourceId', index: 'resource_id', width: 80 }, 
			{ label: '${lang.spm_superpositionmodel_column_url_path}', name: 'urlPath', index: 'url_path', width: 80 }, 
			{ label: '${lang.spm_superpositionmodel_column_url_key}', name: 'urlKey', index: 'url_key', width: 80 }, 
			{ label: '${lang.spm_superpositionmodel_column_spm_value}', name: 'spmValue', index: 'spm_value', width: 160,
				formatter: function(value, options, row){
					return '<a href="/?spm='+value +'">'+value +'</a>';
				}
			},
			{ label: '${lang.spm_superpositionmodel_column_forbidden}', name: 'forbidden', index: 'forbidden', width: 50,
				formatter: function(value, options, row){
				return value === 0 ?
					'<span class="label label-success">${lang.sys_string_no}</span>' :
					'<span class="label label-info">${lang.sys_string_yes}</span>';
				}
			},
			{ label: '${lang.spm_superpositionmodel_column_need_login}', name: 'needLogin', index: 'need_login', width: 50,
				formatter: function(value, options, row){
					return value === 0 ?
					'<span class="label label-success">${lang.sys_string_no}</span>' :
					'<span class="label label-info">${lang.sys_string_yes}</span>';
				}
			},
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
		superPositionModel: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "${lang.sys_string_add}";
			vm.superPositionModel = {};
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
			var url = vm.superPositionModel.id == null ? "spm/superpositionmodel/save" : "spm/superpositionmodel/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.superPositionModel),
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
				    url: baseURL + "spm/superpositionmodel/delete",
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
			$.get(baseURL + "spm/superpositionmodel/info/"+id, function(r){
                vm.superPositionModel = r.superPositionModel;
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