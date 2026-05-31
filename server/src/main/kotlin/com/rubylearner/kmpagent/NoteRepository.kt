package com.rubylearner.kmpagent

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object Notes : Table("notes") {
    val id = long("id").autoIncrement()
    val title = varchar("title", 255)
    val content = text("content")
    override val primaryKey = PrimaryKey(id)
}

class NoteRepository(jdbcUrl: String = "jdbc:sqlite:notes.db") {
    init {
        Database.connect(jdbcUrl, driver = "org.sqlite.JDBC")
        transaction { SchemaUtils.create(Notes) }
    }

    fun all(): List<Note> = transaction {
        Notes.selectAll().map(::rowToNote)
    }

    fun find(id: Long): Note? = transaction {
        Notes.selectAll().where { Notes.id eq id }.singleOrNull()?.let(::rowToNote)
    }

    fun create(draft: NoteDraft): Note = transaction {
        val newId = Notes.insert {
            it[title] = draft.title
            it[content] = draft.content
        } get Notes.id
        Note(newId, draft.title, draft.content)
    }

    fun update(id: Long, draft: NoteDraft): Note? = transaction {
        val rows = Notes.update({ Notes.id eq id }) {
            it[title] = draft.title
            it[content] = draft.content
        }
        if (rows == 0) null else Note(id, draft.title, draft.content)
    }

    fun delete(id: Long): Boolean = transaction {
        Notes.deleteWhere { Notes.id eq id } > 0
    }

    private fun rowToNote(row: ResultRow) = Note(
        id = row[Notes.id],
        title = row[Notes.title],
        content = row[Notes.content],
    )
}
