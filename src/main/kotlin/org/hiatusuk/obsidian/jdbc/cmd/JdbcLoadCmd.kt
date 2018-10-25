package org.hiatusuk.obsidian.jdbc.cmd

import com.mysql.cj.jdbc.MysqlDataSource
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.cmd.api.Validate
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.jdbc.delegates.JdbcEnvironments
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection
import org.postgresql.ds.common.BaseDataSource
import org.slf4j.Logger
import java.io.File
import java.io.FileReader
import java.io.IOException
import javax.inject.Inject

@ScenarioScope
@Command("jdbc:load")
class JdbcLoadCmd @Inject
internal constructor(private val envts: JdbcEnvironments, private val log: Logger) : CommandIF {

    @Validate
    fun checkArgs(inCmd: CommandSpec) {
        val f = getInputFile(inCmd)
        require(f.exists()) {"Input file doesn't exist: $f"}
    }

    @Throws(IOException::class)
    override fun run(inCmd: CommandSpec) {
        val destTable = inCmd.getString("into")

        val ds = envts["main"]

        when (ds) {
            is BaseDataSource ->
                ds.getConnection().use {

                    val f = getInputFile(inCmd)
                    log.info("Loading data into `{}` from '{}'...", destTable, f)

                    val copyManager = CopyManager(it as BaseConnection)

                    FileReader(f).use { fileReader ->
                        copyManager.copyIn("COPY $destTable FROM STDIN (DELIMITER '|', NULL 'NULL')", fileReader)
                        log.info("Copying done")
                    }
                }

            is MysqlDataSource ->
                ds.getConnection().use {
                    it.createStatement().use { s ->
                        val f = getInputFile(inCmd)
                        log.info("Loading data into `{}` from '{}'...", destTable, f)
                        s.executeUpdate("LOAD DATA LOCAL INFILE '$f' INTO TABLE $destTable FIELDS TERMINATED BY '|'")
                    }
                }

            else -> throw RuntimeException("Unsupported DataSource type")
        }
    }

    private fun getInputFile(inCmd: CommandSpec): File {
        val input = inCmd.getString("from")
        require(input.startsWith("file:")) {"File inputs ('$input') should start with 'file:'"}  // FIXME Make more flexible
        return File(input.substring(5))  // Strip off 'file:' prefix. Bit fugly.
    }
}
