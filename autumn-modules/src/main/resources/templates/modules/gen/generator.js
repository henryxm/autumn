$(function () {
    $("#jqGrid").jqGrid({
        datatype: "local",
        data: [],
        colModel: [			
			{ label: '${lang.sys_string_table_name}', name: 'tableName', width: 100, key: true },
			{ label: '${lang.sys_string_engine}', name: 'engine', width: 70},
			{ label: '${lang.sys_string_table_comment}', name: 'tableComment', width: 100 },
			{ label: '${lang.sys_string_create_time}', name: 'createTime', width: 100 }
        ],
		viewrecords: true,
        height: "auto",
        rowNum: 100,
		rowList : [10,30,50,100,200,500,1000],
        rownumbers: true, 
        rownumWidth: 25, 
        autowidth:true,
        multiselect: true,
        pager: "#jqGridPager",
        gridComplete:function(){
        	//隐藏grid底部滚动条
        	$("#jqGrid").closest(".ui-jqgrid-bdiv").css({ "overflow-x" : "hidden" }); 
        }
    });
});

var vm = new Vue({
	el:'#rrapp',
	data:{
        allTables: [],
		q:{
			tableName: null,
            tablePrefix: "",
            tablePrefixList: [],
            genType:null,
            moduleText:"${lang.sys_string_select_generator_solution}",
            genId:null,
            genTypeList:[],
		}
	},
    computed: {
        hasPrefixFilter: function () {
            return !!this.q.tablePrefix;
        }
    },
	methods: {
		query: function () {
            vm.applyFilter();
		},
		generator: function() {
		    if(null == vm.q.genId){
		        alert("${lang.sys_string_select_generator_solution}");
		        return;
            }

			var tableNames = getSelectedRows();
			if(tableNames == null){
				return ;
			}
            location.href = baseURL + "gen/generator/code?tables=" + tableNames.join()+"&genId="+vm.q.genId;
		},
        resetTable: function() {
            var tableNames = getSelectedRows();
            if(tableNames == null){
                tableNames = [] ;
            }
            confirm('${lang.sys_string_are_sure_to_reset_table}？', function(){
                $.ajax({
                    type: "POST",
                    url: baseURL + "gen/generator/reset",
                    contentType: "application/json",
                    data: JSON.stringify(tableNames),
                    success: function(r){
                        if(r.code == 0){
                            alert('${lang.sys_string_success}', function(){
                                vm.reload();
                            });
                        }else{
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        selectType: function(a) {
           vm.q.genType = vm.q.genTypeList[a];
           vm.q.genId = vm.q.genType.id;
           vm.q.moduleText = vm.q.genType.moduleText;
        },
        selectPrefixTables: function() {
            if (!vm.hasPrefixFilter) {
                alert("请先选择表前缀");
                return;
            }
            var selectedPrefix = vm.q.tablePrefix;
            var ids = $("#jqGrid").jqGrid("getDataIDs");
            if (!ids || ids.length === 0) {
                return;
            }
            $("#jqGrid").jqGrid("resetSelection");
            for (var i = 0; i < ids.length; i++) {
                var row = $("#jqGrid").jqGrid("getRowData", ids[i]);
                if (vm.matchPrefix(row.tableName, selectedPrefix)) {
                    $("#jqGrid").jqGrid("setSelection", ids[i], false);
                }
            }
        },
        reload: function() {
            vm.loadAllTables();
        },
        loadAllTables: function() {
            var allRows = [];
            var pageSize = 500;
            var loadByPage = function (pageNo) {
                $.get(baseURL + "gen/generator/list", {page: pageNo, limit: pageSize}, function (data) {
                    var pageData = (data && data.page) ? data.page : {};
                    var list = pageData.list || [];
                    allRows = allRows.concat(list);
                    var totalPage = pageData.totalPage || 1;
                    if (pageNo < totalPage) {
                        loadByPage(pageNo + 1);
                    } else {
                        vm.allTables = allRows;
                        vm.buildPrefixList();
                        vm.applyFilter();
                    }
                });
            };
            loadByPage(1);
        },
        buildPrefixList: function() {
            var prefixes = {};
            for (var i = 0; i < vm.allTables.length; i++) {
                var tableName = vm.allTables[i].tableName;
                if (!tableName) {
                    continue;
                }
                var prefix = vm.extractPrefix(tableName);
                if (prefix) {
                    prefixes[prefix] = true;
                }
            }
            vm.q.tablePrefixList = Object.keys(prefixes).sort();
            if (vm.q.tablePrefix && !prefixes[vm.q.tablePrefix]) {
                vm.q.tablePrefix = "";
            }
        },
        extractPrefix: function(tableName) {
            var firstUnderscore = tableName.indexOf("_");
            if (firstUnderscore > 0) {
                return tableName.substring(0, firstUnderscore);
            }
            return tableName;
        },
        matchPrefix: function(tableName, prefix) {
            if (!prefix) {
                return true;
            }
            if (tableName === prefix) {
                return true;
            }
            return tableName.indexOf(prefix + "_") === 0;
        },
        applyFilter: function() {
            var keyword = vm.q.tableName ? vm.q.tableName.toLowerCase() : "";
            var prefix = vm.q.tablePrefix;
            var rows = [];
            for (var i = 0; i < vm.allTables.length; i++) {
                var row = vm.allTables[i];
                var tableName = row.tableName || "";
                if (prefix && !vm.matchPrefix(tableName, prefix)) {
                    continue;
                }
                if (keyword && tableName.toLowerCase().indexOf(keyword) < 0) {
                    continue;
                }
                rows.push(row);
            }
            $("#jqGrid").jqGrid("clearGridData");
            $("#jqGrid").jqGrid("setGridParam", {
                data: rows,
                page: 1
            }).trigger("reloadGrid");
        }
	},
    mounted: function () {
        this.$nextTick(function () {
            $.get(baseURL + "gen/gentype/list",function (data) {
                vm.q.genTypeList = data.page.list;
            });
            vm.loadAllTables();
        });
    }
});

