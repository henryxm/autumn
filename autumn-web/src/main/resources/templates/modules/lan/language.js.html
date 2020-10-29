$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'lan/language/list',
        datatype: "json",
        colModel: [			
			{ label: 'id', name: 'id', index: 'id', width: 50, key: true },
			{ label: '${lang.lan_language_column_name}', name: 'name', index: 'name', width: 80 }, 			
			{ label: '${lang.lan_language_column_zh_cn}', name: 'zhCn', index: 'zh_cn', width: 80 }, 			
			{ label: '${lang.lan_language_column_en_us}', name: 'enUs', index: 'en_us', width: 80 }, 			
			{ label: '${lang.lan_language_column_zh_hk}', name: 'zhHk', index: 'zh_hk', width: 80 }, 			
			{ label: '${lang.lan_language_column_ko_kr}', name: 'koKr', index: 'ko_kr', width: 80 }, 			
			{ label: '${lang.lan_language_column_ja_jp}', name: 'jaJp', index: 'ja_jp', width: 80 }, 			
			{ label: '${lang.lan_language_column_tt_ru}', name: 'ttRu', index: 'tt_ru', width: 80 }, 			
			{ label: '${lang.lan_language_column_fr_fr}', name: 'frFr', index: 'fr_fr', width: 80 }, 			
			{ label: '${lang.lan_language_column_de_de}', name: 'deDe', index: 'de_de', width: 80 }, 			
			{ label: '${lang.lan_language_column_vi_vn}', name: 'viVn', index: 'vi_vn', width: 80 }, 			
			{ label: '${lang.lan_language_column_th_th}', name: 'thTh', index: 'th_th', width: 80 }, 			
			{ label: '${lang.lan_language_column_ms_my}', name: 'msMy', index: 'ms_my', width: 80 }, 			
			{ label: '${lang.lan_language_column_id_id}', name: 'idId', index: 'id_id', width: 80 }, 			
			{ label: '${lang.lan_language_column_es_es}', name: 'esEs', index: 'es_es', width: 80 }, 			
			{ label: '${lang.lan_language_column_tr_tr}', name: 'trTr', index: 'tr_tr', width: 80 }, 			
			{ label: '${lang.lan_language_column_uk_uk}', name: 'ukUk', index: 'uk_uk', width: 80 }, 			
			{ label: '${lang.lan_language_column_pu_pt}', name: 'puPt', index: 'pu_pt', width: 80 }, 			
			{ label: '${lang.lan_language_column_pl_pl}', name: 'plPl', index: 'pl_pl', width: 80 }, 			
			{ label: '${lang.lan_language_column_mn_mn}', name: 'mnMn', index: 'mn_mn', width: 80 }, 			
			{ label: '${lang.lan_language_column_nb_no}', name: 'nbNo', index: 'nb_no', width: 80 }, 			
			{ label: '${lang.lan_language_column_it_it}', name: 'itIt', index: 'it_it', width: 80 }, 			
			{ label: '${lang.lan_language_column_he_il}', name: 'heIl', index: 'he_il', width: 80 }, 			
			{ label: '${lang.lan_language_column_el_gr}', name: 'elGr', index: 'el_gr', width: 80 }, 			
			{ label: '${lang.lan_language_column_fa_ir}', name: 'faIr', index: 'fa_ir', width: 80 }, 			
			{ label: '${lang.lan_language_column_ar_sa}', name: 'arSa', index: 'ar_sa', width: 80 }, 			
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
			vm.title = "${lang.sys_string_add}";
			vm.language = {};
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
			var url = vm.language.id == null ? "lan/language/save" : "lan/language/update";
			$.ajax({
				type: "POST",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.language),
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
				    url: baseURL + "lan/language/delete",
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