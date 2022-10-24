package entry

import cn.hutool.core.util.StrUtil

fun String.underscoreToCamel(): String {
    val names = this.split("_")
    val sb = StringBuilder()
    for (n in names) {
        sb.append(n.firstToUpper())
    }
    return sb.toString()
}

fun String.fmtName(): String {
    var name = this.clearName().firstToUpper()
    if (name.contains("_")) name = name.underscoreToCamel()
    return name
    // return name.replace("Id".toRegex(), "ID")
}

fun String.clearName() =
    this.replace("`".toRegex(), "").replace("'".toRegex(), "").replace("\"".toRegex(), "")


fun String.firstToUpper(): String {
    if (this.isEmpty()) return ""
    val ch = this.toCharArray()
    ch[0] = ch[0].toUpperCase()
    return String(ch)
}

// tpl should: `json:"%s" bson:"%s" etc...`
fun String.makeTags(tpl: String): String {
    if (tpl.isEmpty()) {
        return ""
    }
    return "    `" + String.format(tpl, StrUtil.toCamelCase(this), this) + "`"
}

fun String.makeDaoFunc(): String {
    val dao = this + "Dao"
    return """
type $dao struct {
    m  *$this
}

func New$dao() *$dao {
	return &$dao{
		m: &$this{},
	}
}
    """.trimIndent()
}

fun String.makeGetByIdFunc(): String {
    val dao = this + "Dao"
    return """
// GetById 根据 ID 查询
func (d *$dao) GetById(ctx context.Context, id int) ($this, error) {
	var result $this
	err := db.Take(&result, id).Error
	return result, err
}""".trimIndent()
}

fun String.makeGetOneFunc(): String {
    val dao = this + "Dao"
    return """
// GetOne 据 where 条件查询一条记录
func (d *$dao) GetOne(ctx context.Context, where map[string]any) ($this, error) {
	var result $this
	err := db.Take(&result, where).Error
	return result, err
}""".trimIndent()
}

fun String.makeListByIdsFunc(): String {
    val dao = this + "Dao"
    return """
// ListByIds 查询（根据ID 批量查询）
func (d *$dao) ListByIds(ctx context.Context, ids []int) ([]$this, error) {
	var result []$this
	if len(ids) == 0 {
		return result, errors.New("ListByIds：ids参数不能为空")
	}
	err := db.Find(&result, ids).Error
	return result, err
}""".trimIndent()
}

fun String.makeListByMapFunc(): String {
    val dao = this + "Dao"
    return """
// ListByMap 根据 where 条件，查询全部记录
func (d *$dao) ListByMap(ctx context.Context, where map[string]any) ([]$this, error) {
	var result []$this
	if len(where) == 0 {
		return result, errors.New("ListByMap：where参数不能为空")
	}
	err := db.Find(&result, where).Error
	return result, err
}""".trimIndent()
}

fun String.makeSaveFunc(): String {
    val dao = this + "Dao"
    return """
// Save 插入一条记录
func (d *$dao) Save(ctx context.Context, obj *$this) error {
	err := db.Create(obj).Error
	if err != nil {
		return err
	}
	return nil
}""".trimIndent()
}

fun String.makeSaveBatchFunc(): String {
    val dao = this + "Dao"
    return """
// SaveBatch 插入（批量）
func (d *$dao) SaveBatch(ctx context.Context, list []$this) error {
	err := db.Create(&list).Error
	if err != nil {
		return err
	}
	return nil
}""".trimIndent()
}

fun String.makeUpdateFunc(): String {
    val dao = this + "Dao"
    return """
// Update 更新
func (d *$dao) Update(ctx context.Context, where string, update map[string]any, args ...any) error {
    err := db.Model(d.m).Where(where, args...).Updates(update).Error
    if err != nil {
        return fmt.Errorf("$dao:Update where=%s: %w", where, err)
    }
    return nil
}
    """.trimIndent()
}

fun String.makeQueryFunc(): String {
    val dao = this + "Dao"
    return """
// Query 原生 SQL 查询
func (d *$dao) Query(ctx context.Context, result any, sql string, args ...any) error {
    if len(sql) == 0 {
        return gorm.ErrInvalidData
    }
    if err := db.Raw(sql, args...).Scan(result).Error; err != nil {
        return fmt.Errorf("$dao: Query sql=%s: %w", sql, err)
    }
    return nil
}
    """.trimIndent()
}

fun String.makeExecFunc(): String {
    val dao = this + "Dao"
    return """
// Exec 原生 SQL 修改
func (d *$dao) Exec(ctx context.Context, sql string, args ...any) error {
    if len(sql) == 0 {
        return gorm.ErrInvalidData
    }
    if err := db.Exec(sql, args...).Error; err != nil {
        return fmt.Errorf("$dao: Exec sql=%s: %w", sql, err)
    }
    return nil
}
    """.trimIndent()
}