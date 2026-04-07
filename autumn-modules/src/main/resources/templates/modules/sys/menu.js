var MENU_ROOT_KEY = "-1";
var MENU_TREE_ROOT_BATCH_SIZE = 200;
var setting = {
    data: {
        simpleData: {
            enable: true,
            idKey: "menuKey",
            pIdKey: "parentKey",
            rootPId: MENU_ROOT_KEY
        },
        key: {
            url:"nourl"
        }
    },
    callback: {
        beforeExpand: function (treeId, treeNode) {
            if (!treeNode) return true;
            ensureNodeChildrenLoaded(treeNode);
            return true;
        }
    }
};
var ztree;
var menuTreeCache = {
    loaded: false,
    list: [],
    nodeById: {},
    childrenByParent: {}
};
var menuTreeLoading = false;

function normalizeMenuId(val) {
    if (val === null || val === undefined) return "";
    return String(val);
}

function isRootParent(parentKey) {
    var p = normalizeMenuId(parentKey);
    return p === "" || p === MENU_ROOT_KEY || p === "0" || p.toLowerCase() === "null";
}

function buildMenuTreeCache(menuList) {
    menuTreeCache.list = menuList || [];
    menuTreeCache.nodeById = {};
    menuTreeCache.childrenByParent = {};
    for (var i = 0; i < menuTreeCache.list.length; i++) {
        var node = menuTreeCache.list[i];
        var id = normalizeMenuId(node.menuKey);
        var parentId = normalizeMenuId(node.parentKey);
        node.menuKey = id;
        node.parentKey = parentId;
        menuTreeCache.nodeById[id] = node;
        var bucket = isRootParent(parentId) ? MENU_ROOT_KEY : parentId;
        if (!menuTreeCache.childrenByParent[bucket]) {
            menuTreeCache.childrenByParent[bucket] = [];
        }
        menuTreeCache.childrenByParent[bucket].push(node);
    }
    menuTreeCache.loaded = true;
}

function toTreeNode(menu) {
    var id = normalizeMenuId(menu.menuKey);
    var children = menuTreeCache.childrenByParent[id] || [];
    return {
        menuKey: id,
        parentKey: normalizeMenuId(menu.parentKey),
        name: menu.name,
        open: false,
        isParent: children.length > 0,
        _childrenLoaded: false
    };
}

function getRootNodes() {
    var roots = menuTreeCache.childrenByParent[MENU_ROOT_KEY] || [];
    var list = [];
    for (var i = 0; i < roots.length; i++) {
        list.push(toTreeNode(roots[i]));
    }
    return list;
}

function ensureNodeChildrenLoaded(treeNode) {
    if (!treeNode || treeNode._childrenLoaded) {
        return;
    }
    var children = menuTreeCache.childrenByParent[normalizeMenuId(treeNode.menuKey)] || [];
    if (children.length > 0) {
        var nodesToAdd = [];
        for (var i = 0; i < children.length; i++) {
            nodesToAdd.push(toTreeNode(children[i]));
        }
        ztree.addNodes(treeNode, nodesToAdd, false);
    }
    treeNode._childrenLoaded = true;
    treeNode.isParent = children.length > 0;
    ztree.updateNode(treeNode);
}

function ensurePathToNodeLoaded(targetMenuKey) {
    var target = normalizeMenuId(targetMenuKey);
    if (!target || target === MENU_ROOT_KEY) return;
    var chain = [];
    var cursor = menuTreeCache.nodeById[target];
    while (cursor) {
        chain.unshift(normalizeMenuId(cursor.menuKey));
        var p = normalizeMenuId(cursor.parentKey);
        if (isRootParent(p)) {
            break;
        }
        cursor = menuTreeCache.nodeById[p];
    }
    for (var i = 0; i < chain.length - 1; i++) {
        var current = ztree.getNodeByParam("menuKey", chain[i]);
        if (!current) break;
        ensureNodeChildrenLoaded(current);
        ztree.expandNode(current, true, false, false);
    }
}

function selectCurrentParentNode() {
    if (vm.menu.parentKey && vm.menu.parentKey !== MENU_ROOT_KEY) {
        ensurePathToNodeLoaded(vm.menu.parentKey);
    }
    var node = ztree.getNodeByParam("menuKey", vm.menu.parentKey || MENU_ROOT_KEY);
    if (!node) {
        node = ztree.getNodeByParam("menuKey", MENU_ROOT_KEY);
    }
    if (node) {
        ztree.selectNode(node);
        vm.menu.parentName = node.name;
    } else {
        vm.menu.parentKey = MENU_ROOT_KEY;
        vm.menu.parentName = "";
    }
}

function renderRootNodesInBatches(roots, done) {
    ztree = $.fn.zTree.init($("#menuTree"), setting, []);
    if (!roots || roots.length === 0) {
        done && done();
        return;
    }
    var idx = 0;
    var chunkSize = MENU_TREE_ROOT_BATCH_SIZE;
    (function appendChunk() {
        var batch = roots.slice(idx, idx + chunkSize);
        if (batch.length > 0) {
            ztree.addNodes(null, batch, false);
        }
        idx += chunkSize;
        if (idx < roots.length) {
            setTimeout(appendChunk, 0);
        } else {
            done && done();
        }
    })();
}

function initMenuTree(done) {
    var roots = getRootNodes();
    if (roots.length <= MENU_TREE_ROOT_BATCH_SIZE) {
        ztree = $.fn.zTree.init($("#menuTree"), setting, roots);
        selectCurrentParentNode();
        done && done();
        return;
    }
    renderRootNodesInBatches(roots, function () {
        selectCurrentParentNode();
        done && done();
    });
}

function ensureMenuTreeReady(callback) {
    if (menuTreeLoading) {
        return;
    }
    menuTreeLoading = true;
    var afterReady = function () {
        menuTreeLoading = false;
        callback && callback();
    };
    if (menuTreeCache.loaded) {
        initMenuTree(afterReady);
        return;
    }
    $.get(baseURL + "sys/menu/select", function(r){
        buildMenuTreeCache(r.menuList);
        initMenuTree(afterReady);
    }).fail(function () {
        menuTreeLoading = false;
        alert("Load menu tree failed");
    });
}

var vm = new Vue({
    el:'#rrapp',
    data:{
        showList: true,
        title: null,
        menu:{
            parentName:null,
            parentKey:"-1",
            type:1,
            orderNum:0
        }
    },
    methods: {
        getMenu: function(menuId){
            // 延迟到真正打开“选择上级菜单”弹窗时初始化树，避免编辑页切换时卡顿。
        },
        add: function(){
            vm.showList = false;
            vm.title = "${lang.sys_string_add}";
            vm.menu = {parentName:null,parentKey:MENU_ROOT_KEY,type:1,orderNum:0};
        },
        update: function () {
            var menuKey = getMenuId();
            if(menuKey == null){
                return ;
            }

            $.get(baseURL + "sys/menu/info/"+menuKey, function(r){
                vm.showList = false;
                vm.title = "${lang.sys_string_change}";
                vm.menu = r.menu;
            });
        },
        del: function () {
            var menuKey = getMenuId();
            if(menuKey == null){
                return ;
            }

            confirm('${lang.sys_string_are_sure_to_delete}？', function(){
                $.ajax({
                    type: "POST",
                    url: baseURL + "sys/menu/delete",
                    data: "menuKey=" + menuKey,
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
        saveOrUpdate: function () {
            if(vm.validator()){
                return ;
            }

            var url = "sys/menu/save";
            $.ajax({
                type: "POST",
                url:  baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.menu),
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
        menuTree: function(){
            ensureMenuTreeReady(function () {
                layer.open({
                    type: 1,
                    offset: '50px',
                    skin: 'layui-layer-molv',
                    title: "${lang.sys_string_select_menu}",
                    area: ['300px', '450px'],
                    shade: 0,
                    shadeClose: false,
                    content: jQuery("#menuLayer"),
                    btn: ['${lang.sys_string_confirm}', '${lang.sys_string_cancel}'],
                    btn1: function (index) {
                        var node = ztree.getSelectedNodes();
                        if(!node || node.length === 0){
                            return;
                        }
                        //选择上级菜单
                        vm.menu.parentKey = node[0].menuKey;
                        vm.menu.parentName = node[0].name;

                        layer.close(index);
                    }
                });
            });
        },
        reload: function () {
            vm.showList = true;
            Menu.initTable();
        },
        validator: function () {
            if(isBlank(vm.menu.name)){
                alert("${lang.sys_string_menu_name_cannot_be_empty}");
                return true;
            }

            //菜单
            if(vm.menu.type === 1 && isBlank(vm.menu.url)){
                alert("${lang.sys_string_menu_url_cannot_be_empty}");
                return true;
            }
        }
    }
});


var Menu = {
    id: "menuTable",
    table: null,
    layerIndex: -1
};

/** 从 /sys/menu/list 解析数组（兼容直接 JSON 数组或分页包装） */
Menu.parseListResponse = function (res) {
    if ($.isArray(res)) return res;
    if (res && $.isArray(res.list)) return res.list;
    if (res && res.page && $.isArray(res.page.list)) return res.page.list;
    return [];
};

/**
 * 按 parentKey 做前序遍历，写入 _treeDepth；用于普通 bootstrapTable + 名称列缩进，避免 TreeTable 一次挂全树 DOM 卡死。
 */
Menu.buildFlatTreeRows = function (list) {
    if (!list || list.length === 0) return [];
    var byKey = {};
    var byParent = {};
    for (var i = 0; i < list.length; i++) {
        var row = list[i];
        var mk = String(row.menuKey);
        byKey[mk] = row;
        var ps = String(row.parentKey);
        if (!byParent[ps]) byParent[ps] = [];
        byParent[ps].push(row);
    }
    function isRoot(row) {
        var pk = row.parentKey;
        if (pk === null || pk === undefined || pk === "") return true;
        var s = String(pk);
        if (s === "-1" || s === "0" || s.toLowerCase() === "null") return true;
        return !byKey[s];
    }
    var roots = [];
    for (var j = 0; j < list.length; j++) {
        if (isRoot(list[j])) roots.push(list[j]);
    }
    if (roots.length === 0) roots = list.slice();
    roots.sort(function (a, b) {
        return (a.orderNum || 0) - (b.orderNum || 0) || String(a.menuKey).localeCompare(String(b.menuKey));
    });
    var ordered = [];
    var visited = {};
    function dfs(node, depth) {
        var id = String(node.menuKey);
        if (visited[id]) return;
        visited[id] = true;
        node._treeDepth = depth;
        ordered.push(node);
        var children = byParent[id] ? byParent[id].slice() : [];
        children.sort(function (a, b) {
            return (a.orderNum || 0) - (b.orderNum || 0) || String(a.menuKey).localeCompare(String(b.menuKey));
        });
        for (var m = 0; m < children.length; m++) dfs(children[m], depth + 1);
    }
    for (var r = 0; r < roots.length; r++) dfs(roots[r], 0);
    for (var n = 0; n < list.length; n++) {
        var row = list[n];
        if (!visited[String(row.menuKey)]) {
            row._treeDepth = 0;
            ordered.push(row);
        }
    }
    return ordered;
};

Menu.escapeHtml = function (s) {
    if (s === null || s === undefined) return "";
    return String(s)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
};

/** 从 TreeTable / 旧版 DOM 中还原出干净的 table，便于切换为 bootstrapTable */
Menu.resetTableDom = function () {
    var $tbl = $("#" + Menu.id);
    try {
        $tbl.bootstrapTable("destroy");
    } catch (e) { /* ignore */ }
    var $bt = $tbl.closest(".bootstrap-table");
    if ($bt.length) {
        $bt.before($tbl);
        $bt.remove();
    }
    var guard = 0;
    while (guard++ < 8 && $tbl.parent().hasClass("fixed-table-container")) {
        var $c = $tbl.parent();
        $c.prev(".fixed-table-toolbar").remove();
        $c.before($tbl);
        $c.remove();
    }
    $tbl.removeClass("table table-hover treegrid-table table-bordered table-striped");
    $tbl.empty();
};

/**
 * 初始化表格的列（bootstrapTable：formatter 签名为 value, row, index）
 */
Menu.initColumn = function () {
    var breakLongText = {
        css: {
            "max-width": "220px",
            "word-break": "break-all",
            "white-space": "normal",
            "vertical-align": "middle"
        }
    };
    var columns = [
        {field: "selectItem", radio: true, width: 40, align: "center", valign: "middle"},
        {
            title: "${lang.sys_string_menu_name}",
            field: "name",
            align: "left",
            valign: "middle",
            sortable: false,
            width: 200,
            cellStyle: function (value, row) {
                var d = row._treeDepth || 0;
                return {css: {"padding-left": 12 + d * 16 + "px", "vertical-align": "middle"}};
            },
            formatter: function (value) {
                return value == null || value === "" ? "" : Menu.escapeHtml(value);
            }
        },
        {
            title: "${lang.sys_string_menu_key}",
            field: "menuKey",
            align: "left",
            valign: "middle",
            sortable: false,
            width: 120,
            cellStyle: function () {
                return breakLongText;
            },
            formatter: function (value) {
                return value == null || value === "" ? "" : Menu.escapeHtml(value);
            }
        },
        {
            title: "${lang.sys_string_upper_menu}",
            field: "parentName",
            align: "left",
            valign: "middle",
            sortable: false,
            width: 120,
            cellStyle: function () {
                return breakLongText;
            },
            formatter: function (value) {
                return value == null || value === "" ? "" : Menu.escapeHtml(value);
            }
        },
        {title: "${lang.sys_string_icon}", field: "icon", align: "center", valign: "middle", sortable: false, width: 72, formatter: function (value) {
            if (value == null || value === "") return "";
            return '<i class="' + Menu.escapeHtml(value) + ' fa-lg"></i>';
        }},
        {
            title: "${lang.sys_string_type}",
            field: "type",
            align: "center",
            valign: "middle",
            sortable: false,
            width: 88,
            cellStyle: function () {
                return {css: {"white-space": "nowrap", "vertical-align": "middle"}};
            },
            formatter: function (value) {
                if (value === 0) return '<span class="label label-primary">${lang.sys_string_directory}</span>';
                if (value === 1) return '<span class="label label-success">${lang.sys_string_menu}</span>';
                if (value === 2) return '<span class="label label-warning">${lang.sys_string_button}</span>';
                return "";
            }
        },
        {title: "${lang.sys_string_order_number}", field: "orderNum", align: "center", valign: "middle", sortable: false, width: 72},
        {
            title: "${lang.sys_string_menu_url}",
            field: "url",
            align: "left",
            valign: "middle",
            sortable: false,
            width: 200,
            cellStyle: function () {
                return breakLongText;
            },
            formatter: function (value) {
                return value == null || value === "" ? "" : Menu.escapeHtml(value);
            }
        },
        {
            title: "${lang.sys_string_permissions}",
            field: "perms",
            align: "left",
            valign: "middle",
            sortable: false,
            width: 180,
            cellStyle: function () {
                return breakLongText;
            },
            formatter: function (value) {
                return value == null || value === "" ? "" : Menu.escapeHtml(value);
            }
        }
    ];
    return columns;
};

/**
 * 列表：bootstrapTable + 前序遍历 + 名称缩进（非 TreeTable，避免巨量树节点卡死）。
 */
Menu.initTable = function () {
    Menu.resetTableDom();
    $.get(baseURL + "sys/menu/list", function (res) {
        var list = Menu.parseListResponse(res);
        var rows = Menu.buildFlatTreeRows(list);
        var columns = Menu.initColumn();
        $("#" + Menu.id).bootstrapTable({
            data: rows,
            columns: columns,
            classes: "table table-bordered table-striped table-hover",
            pagination: true,
            pageSize: 50,
            pageList: [20, 50, 100, 200],
            search: true,
            searchOnEnterKey: true,
            showColumns: true,
            showRefresh: true,
            clickToSelect: true,
            idField: "menuKey",
            sidePagination: "client",
            onRefresh: function () {
                setTimeout(function () {
                    Menu.initTable();
                }, 0);
            }
        });
        Menu.table = $("#" + Menu.id);
    }).fail(function () {
        alert("Load menu list failed");
    });
};


function getMenuId () {
    var selected = $("#menuTable").bootstrapTable("getSelections");
    if (selected.length === 0) {
        alert("${lang.sys_string_please_select_record}");
        return null;
    } else {
        return selected[0].menuKey || selected[0].id;
    }
}


$(function () {
    Menu.initTable();
});
