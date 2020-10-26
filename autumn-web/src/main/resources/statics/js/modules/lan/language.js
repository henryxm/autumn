$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'lan/language/list',
        datatype: "json",
        colModel: [			
			{ label: 'id', name: 'id', index: 'id', width: 50, key: true },
			{ label: '标识', name: 'name', index: 'name', width: 80 }, 			
			{ label: '英语(美国)', name: 'enUs', index: 'en_us', width: 80 }, 			
			{ label: '简体中文(中国)', name: 'zhCn', index: 'zh_cn', width: 80 }, 			
			{ label: '繁体中文(香港)', name: 'zhHk', index: 'zh_hk', width: 80 }, 			
			{ label: '韩语(韩国)', name: 'koKr', index: 'ko_kr', width: 80 }, 			
			{ label: '日语(日本)', name: 'jaJp', index: 'ja_jp', width: 80 }, 			
			{ label: '俄语(俄罗斯)', name: 'ttRu', index: 'tt_ru', width: 80 }, 			
			{ label: '法语(法国)', name: 'frFr', index: 'fr_fr', width: 80 }, 			
			{ label: '德语(德国)', name: 'deDe', index: 'de_de', width: 80 }, 			
			{ label: '越语(越南)', name: 'viVn', index: 'vi_vn', width: 80 }, 			
			{ label: '泰语(泰国)', name: 'thTh', index: 'th_th', width: 80 }, 			
			{ label: '马来语(马来西亚)', name: 'msMy', index: 'ms_my', width: 80 }, 			
			{ label: '印尼语(印尼)', name: 'idId', index: 'id_id', width: 80 }, 			
			{ label: '西班牙语(西班牙)', name: 'esEs', index: 'es_es', width: 80 }, 			
			{ label: '土耳其语(土耳其)', name: 'trTr', index: 'tr_tr', width: 80 }, 			
			{ label: '乌克兰语(乌克兰)', name: 'ukUk', index: 'uk_uk', width: 80 }, 			
			{ label: '葡萄牙语(葡萄牙)', name: 'puPt', index: 'pu_pt', width: 80 }, 			
			{ label: '波兰语(波兰)', name: 'plPl', index: 'pl_pl', width: 80 }, 			
			{ label: '蒙古语(蒙古)', name: 'mnMn', index: 'mn_mn', width: 80 }, 			
			{ label: '挪威语(挪威)', name: 'nbNo', index: 'nb_no', width: 80 }, 			
			{ label: '意大利语(意大利)', name: 'itIt', index: 'it_it', width: 80 }, 			
			{ label: '希伯来语(以色列)', name: 'heIl', index: 'he_il', width: 80 }, 			
			{ label: '希腊语(希腊)', name: 'elGr', index: 'el_gr', width: 80 }, 			
			{ label: '波斯语(伊朗)', name: 'faIr', index: 'fa_ir', width: 80 }, 			
			{ label: '阿拉伯语(沙特阿拉伯)', name: 'arSa', index: 'ar_sa', width: 80 }, 			
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
		language: {}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function(){
			vm.showList = false;
			vm.title = "新增";
			vm.language = {};
		},
		update: function (event) {
			var id = getSelectedRow();
			if(id == null){
				return ;
			}
			vm.showList = false;
            vm.title = "修改";
            
            vm.getInfo(id)
		},
		saveOrUpdate: function (event) {
			var url = vm.language.id == null ? "lan/language/save" : "lan/language/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.language),
			    success: function(r){
			    	if(r.code === 0){
						alert('操作成功', function(index){
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
			
			confirm('确定要删除选中的记录？', function(){
				$.ajax({
					type: "POST",
				    url: baseURL + "lan/language/delete",
                    contentType: "application/json",
				    data: JSON.stringify(ids),
				    success: function(r){
						if(r.code == 0){
							alert('操作成功', function(index){
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
			$.get(baseURL + "lan/language/info/"+id, function(r){
                vm.language = r.language;
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