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
    return name.replace("Id".toRegex(), "ID")
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
    db *gorm.DB
    m  *$this
}

func New$dao(ctx context.Context, db *gorm.DB) *$dao {
    dao := new($dao)
    dao.db = db
    return dao
}
    """.trimIndent()
}

fun String.makeCreateFunc(): String {
    val dao = this + "Dao"
    return """
func (d *$dao) Create(ctx context.Context, obj *$this) error {
	err := d.db.Model(d.m).Create(&obj).Error
	if err != nil {
		return err
	}
	return nil
}"""
}


fun String.makeUpdateFunc(): String {
    val dao = this + "Dao"
    return """
func (d *$dao) Update(ctx context.Context, where string, update map[string]any, args ...any) error {
    err := d.db.Model(d.m).Where(where, args...).
        Updates(update).Error
    if err != nil {
        return fmt.Errorf("$dao:Update where=%s: %w", where, err)
    }
    return nil
}
    """.trimIndent()
}

fun String.makeGetFunc(): String {
    val dao = this + "Dao"
    return """
func (d *$dao) Get(ctx context.Context, fields, where string) (*$this, error) {
    items, err := d.List(ctx, fields, where, 0, 1)
    if err != nil {
        return nil, fmt.Errorf("$dao: Get where=%s: %w", where, err)
    }
    if len(items) == 0 {
        return nil, gorm.ErrRecordNotFound
    }
    return &items[0], nil
}
    """.trimIndent()
}

fun String.makeListFunc(): String {
    val dao = this + "Dao"
    return """
func (d *$dao) List(ctx context.Context, fields, where string, offset, limit int) ([]$this, error) {
    var results []$this
    err := d.db.Model(d.m).
        Select(fields).Where(where).Offset(offset).Limit(limit).Find(&results).Error
    if err != nil {
        return nil, fmt.Errorf("$dao: List where=%s: %w", where, err)
    }
    return results, nil
}
    """.trimIndent()
}

fun String.makeDeleteFunc(): String {
    val dao = this + "Dao"
    return """
func (d *$dao) Delete(ctx context.Context, where string, args ...any) error {
    if len(where) == 0 {
        return gorm.ErrMissingWhereClause
    }
    if err := d.db.Where(where, args...).Update("status", 0).Error; err != nil {
        return fmt.Errorf("$dao: Delete where=%s: %w", where, err)
    }
    return nil
}
    """.trimIndent()
}

fun String.makeQueryFunc(): String {
    val dao = this + "Dao"
    return """
func (d *$dao) Query(ctx context.Context, sql string, args ...any) ([]T, error) {
    var results []T
    if len(sql) == 0 {
        return results, gorm.ErrInvalidData
    }
    if err := d.db.Raw(sql, args...).Scan(results).Error; err != nil {
        return results, fmt.Errorf("$dao: Query sql=%s: %w", sql, err)
    }
    return results, nil
}
    """.trimIndent()
}

fun String.makeExecFunc(): String {
    val dao = this + "Dao"
    return """
func (d *$dao) Exec(ctx context.Context, sql string, args ...any) error {
    if len(sql) == 0 {
        return gorm.ErrInvalidData
    }
    if err := d.db.Exec(sql, args...).Error; err != nil {
        return fmt.Errorf("$dao: Exec sql=%s: %w", sql, err)
    }
    return nil
}
    """.trimIndent()
}