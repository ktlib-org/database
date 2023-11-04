package adapters.db.migration

import org.ktlib.db.Database
import org.ktlib.db.KotlinMigration
import org.ktlib.entities.transaction

class M20220910112756_setup : KotlinMigration() {
    override fun migrate() {
        Database.execute("insert into something (id, name, enabled) values ('fe8e24a6-8c95-4b6d-9c19-2d4d9ca8fa7b', 'FirstValue', true)")
        Database.execute("insert into something_else (id, name, something_id) values ('9ac9fbaf-fbab-4a95-9e3f-459d9bc29a28', 'FirstElse', 'fe8e24a6-8c95-4b6d-9c19-2d4d9ca8fa7b')")
    }

    override fun getChecksum() = 520560888
}