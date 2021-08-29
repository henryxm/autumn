var setting = {
    data: {
        simpleData: {
            enable: true,
            idKey: "deptKey",
            pIdKey: "parentKey",
            rootPId: ""
        },
        key: {
            url:"nourl"
        }
    }
};
var ztree;

var vm = new Vue({
    el:'#rrapp',
    data:{
        showList: true,
        title: null,
        dept:{
            parentName:null,
            parentKey:0,
            orderNum:0
        }
    },
    methods: {
        getDept: function(){
            //加载部门树
            $.get(baseURL + "sys/dept/select", function(r){
                ztree = $.fn.zTree.init($("#deptTree"), setting, r.deptList);
                var node = ztree.getNodeByParam("deptKey", vm.dept.parentKey);
                ztree.selectNode(node);
                vm.dept.parentName = node.name;
            })
        },
        add: function(){
            vm.showList = false;
            vm.title = "${lang.sys_string_add}";
            vm.dept = {parentName:null,parentKey:0,orderNum:0};
            vm.getDept();
        },
        update: function () {
            var deptKey = getDeptKey();
            if(deptKey == null){
                return ;
            }

            $.get(baseURL + "sys/dept/info/"+deptKey, function(r){
                vm.showList = false;
                vm.title = "${lang.sys_string_change}";
                vm.dept = r.dept;

                vm.getDept();
            });
        },
        del: function () {
            var deptKey = getDeptKey();
            if(deptKey == null){
                return ;
            }

            confirm('${lang.sys_string_are_sure_to_delete}？', function(){
                $.ajax({
                    type: "POST",
                    url: baseURL + "sys/dept/delete",
                    data: "deptKey=" + deptKey,
                    success: function(r){
                        if(r.code === 0){
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
        saveOrUpdate: function (event) {
            var url = vm.dept.deptKey == null ? "sys/dept/save" : "sys/dept/update";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.dept),
                success: function(r){
                    if(r.code === 0){
                        alert('${lang.sys_string_success}', function(){
                            vm.reload();
                        });
                    }else{
                        alert(r.msg);
                    }
                }
            });
        },
        deptTree: function(){
            layer.open({
                type: 1,
                offset: '50px',
                skin: 'layui-layer-molv',
                title: "${lang.sys_string_select_department}",
                area: ['300px', '450px'],
                shade: 0,
                shadeClose: false,
                content: jQuery("#deptLayer"),
                btn: ['${lang.sys_string_confirm}', '${lang.sys_string_cancel}'],
                btn1: function (index) {
                    var node = ztree.getSelectedNodes();
                    //选择上级部门
                    vm.dept.parentKey = node[0].deptKey;
                    vm.dept.parentName = node[0].name;
                    layer.close(index);
                }
            });
        },
        reload: function () {
            vm.showList = true;
            Dept.table.refresh();
        }
    }
});

var Dept = {
    id: "deptTable",
    table: null,
    layerIndex: -1
};

/**
 * 初始化表格的列
 */
Dept.initColumn = function () {
    var columns = [
        {field: 'selectItem', radio: true},
        {title: '${lang.sys_string_department_key}', field: 'deptKey', align: 'center', valign: 'middle', sortable: true, width: '150px'},
        {title: '${lang.sys_string_department_name}', field: 'name', align: 'center', valign: 'middle', sortable: true, width: '150px'},
        {title: '${lang.sys_string_upper_department}', field: 'parentName', align: 'center', valign: 'middle', sortable: true, width: '100px'},
        {title: '${lang.sys_string_order_number}', field: 'orderNum', align: 'center', valign: 'middle', sortable: true, width: '50px'},
        {title: '${lang.sys_string_remark}', field: 'remark', align: 'center', valign: 'middle', sortable: true, width: '100px'}]
    return columns;
};


function getDeptKey () {
    var selected = $('#deptTable').bootstrapTreeTable('getSelections');
    if (selected.length == 0) {
        alert("${lang.sys_string_please_select_record}");
        return null;
    } else {
        return selected[0].id;
    }
}


$(function () {
    $.get(baseURL + "sys/dept/info", function(r){
        var colunms = Dept.initColumn();
        var table = new TreeTable(Dept.id, baseURL + "sys/dept/list", colunms);
        table.setRootCodeValue(r.deptKey);
        table.setExpandColumn(2);
        table.setIdField("deptKey");
        table.setCodeField("deptKey");
        table.setParentCodeField("parentKey");
        table.setExpandAll(false);
        table.init();
        Dept.table = table;
    });
});
