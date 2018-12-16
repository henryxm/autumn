$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL +'gen/generator/list',
        datatype: "json",
        colModel: [			
			{ label: '表名', name: 'tableName', width: 100, key: true },
			{ label: 'Engine', name: 'engine', width: 70},
			{ label: '表备注', name: 'tableComment', width: 100 },
			{ label: '创建时间', name: 'createTime', width: 100 }
        ],
		viewrecords: true,
        height: 385,
        rowNum: 100,
		rowList : [10,30,50,100,200],
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
			tableName: null,
            genType:null,
            moduleText:"选择代码生成方案",
            genId:null,
            genTypeList:[],
		}
	},
	methods: {
		query: function () {
			$("#jqGrid").jqGrid('setGridParam',{ 
                postData:{'tableName': vm.q.tableName},
                page:1 
            }).trigger("reloadGrid");
		},
		generator: function() {
		    if(null == vm.q.genId){
		        alert("请选择代码生成方案");
		        return;
            }

			var tableNames = getSelectedRows();
			if(tableNames == null){
				return ;
			}
            location.href = baseURL + "gen/generator/code?tables=" + tableNames.join()+"&genId="+vm.q.genId;
		},
        selectType: function(a) {
           vm.q.genType = vm.q.genTypeList[a];
           vm.q.genId = vm.q.genType.id;
           vm.q.moduleText = vm.q.genType.moduleText;
        }
	},
    mounted: function () {
        this.$nextTick(function () {
            $.get(baseURL + "gen/gentype/list",function (data) {
                vm.q.genTypeList = data.page.list;
            })
        });
    }
});

