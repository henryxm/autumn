package cn.org.autumn.modules.gen.service;

import cn.org.autumn.modules.gen.utils.GenUtils;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.table.TableInit;
import cn.org.autumn.table.dao.TableDao;
import cn.org.autumn.table.mysql.ColumnMeta;
import cn.org.autumn.table.mysql.TableMeta;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.plugins.pagination.PageHelper;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器
 */
@Service
public class GeneratorService {

	@Autowired
	private SysMenuService sysMenuService;

	@Autowired
	private TableDao generatorDao;

	@Autowired
	private TableInit tableInit;

	public PageUtils queryPage(Map<String, Object> params) {
		Page<TableMeta> page = new Page<>();
		page.setRecords( queryList(new Query(params)) );
		return new PageUtils(page);
	}

	public List<TableMeta> queryList(Query query) {
		PageHelper.startPage(query.getPage().getSize(), query.getLimit());
		String tableName = "";
		int offset = 0;
		int limit = 10;
		if (query.containsKey("limit"))
			limit = (int) query.get("limit");
		if (query.containsKey("offset"))
			offset = (int) query.get("offset");
		if (query.containsKey("tableName"))
			tableName = (String) query.get("tableName");

		List<TableMeta> list = generatorDao.getTableMetas(tableName, offset, limit);

		return list;
	}

	public List<TableMeta> queryTable(String tableName) {
		return generatorDao.getTableMetas(tableName);
	}

	public List<ColumnMeta> queryColumns(String tableName) {
		return generatorDao.getColumnMetas(tableName);
	}

	public byte[] generatorCode(String[] tableNames) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(outputStream);

		for(String tableName : tableNames){
			//查询表信息
			List<TableMeta> table = queryTable(tableName);
			//查询列信息
			List<ColumnMeta> columns = queryColumns(tableName);
			//生成代码
			GenUtils.generatorCode(table, columns, zip);
		}
		IOUtils.closeQuietly(zip);
		return outputStream.toByteArray();
	}

	/**
	 * need implement it in the subclass.
	 * @return
	 */
	public int menuOrder(){
		return 13;
	}

	/**
	 * need implement it in the subclass.
	 * @return
	 */
	public int parentMenu(){
		return 1;
	}

	private String order(){
		return String.valueOf(menuOrder());
	}

	private String parent(){
		return String.valueOf(parentMenu());
	}

	@PostConstruct
	public void init() {
		if(!tableInit.init)
			return;
		Long id = 0L;
		String[] _m = new String[]
				{null, parent(), "代码生成", "modules/gen/generator.html", "gen:generator:list,gen:generator:code", "1", "fa fa-file-code-o", order()};
		SysMenuEntity sysMenu = sysMenuService.from(_m);
		SysMenuEntity entity = sysMenuService.get(sysMenu);
		if (null == entity) {
			int ret = sysMenuService.put(sysMenu);
			if (1 == ret)
				id = sysMenu.getMenuId();
		} else
			id = entity.getMenuId();
		String[][] menus = new String[][]{
				{null, id + "", "查看", null, "gen:generator:list,gen:generator:info", "2", null, order()},
				{null, id + "", "生成", null, "gen:generator:code", "2", null, order()},
		};
		for (String[] menu : menus) {
			sysMenu = sysMenuService.from(menu);
			entity = sysMenuService.get(sysMenu);
			if (null == entity) {
				sysMenuService.put(sysMenu);
			}
		}
	}
}
