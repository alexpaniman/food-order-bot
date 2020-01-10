package org.order.logic.impl.commands.registration

import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.order.bot.send.button
import org.order.bot.send.inline
import org.order.bot.send.reply
import org.order.data.entities.*
import org.order.data.entities.State.*
import org.order.data.tables.Relations
import org.order.data.tables.Students
import org.order.logic.commands.processors.CallbackProcessor
import org.order.logic.commands.triggers.StateTrigger
import org.order.logic.commands.TriggerCommand
import org.order.logic.corpus.Text

val CHECK_REGISTRATION = TriggerCommand(trigger = StateTrigger(REGISTRATION_FINISHED)) { user, _ ->
    user.send(Text.get("registration-summary") {
        it["description"] = user.buildDescription(Student, Teacher, Parent, Producer)
    }) {
        reply {
            button(Text["registration-confirm-button"])
            button(Text["registration-dismiss-button"])
        }
    }
    user.state = CONFIRM_REGISTRATION
}


private val REGISTRATION_PROCESSOR_TRIGGER = StateTrigger(CONFIRM_REGISTRATION)
val REGISTRATION_PROCESSOR = TriggerCommand(REGISTRATION_PROCESSOR_TRIGGER) { user, update ->
    when (update.message?.text) {
        Text["registration-confirm-button"] -> {
            val description = user.buildDescription(Student, Teacher, Parent, Producer)

            val targets = Admin.all().map { it.user } + when {
                user.hasLinked(Student) -> user.linked(Student).coordinators
                        .map { it.user }

                user.hasLinked(Parent) -> user.linked(Parent).children
                        .map { it.coordinators }
                        .flatten()
                        .map { it.user }

                user.hasLinked(Producer) -> Producer.all()
                        .map { it.user }

                user.hasLinked(Teacher) -> listOf()

                else -> error("user has no appropriate linked role")
            }

            val message = Text.get("validation") {
                it["description"] = description
            }

            for (target in targets)
                target.send(message) {
                    inline {
                        button(Text["validation-ban"], "validation:ban:${user.id.value}")
                        button(Text["validation-confirm"], "validation:confirm:${user.id.value}")
                        button(Text["validation-dismiss"], "validation:dismiss:${user.id.value}")
                    }
                }

            user.send(Text.get("registration-confirmed") {
                it["description"] = description
            })

            user.state = VALIDATION
        }

        Text["registration-dismiss-button"] -> {
            user.clear()

            user.send(Text["registration-dismissed"])

            user.state = READ_NAME
            user.send(Text["register-name"])
        }

        else -> user.send(Text["wrong-registration-confirmation"])
    }
}

private fun Student.findSameStudent(imagine: Boolean) = Student
        .find { Students.grade eq grade!!.id }
        .firstOrNull {
            it.user.name == user.name && // The student has the same name
                    it.grade == grade && // The student is in the same grade
                    user.valid && // User has passed validation
                    it != this && // The student isn't this student
                    (it.user.state != IMAGINE) xor imagine // According to imagine variable student imagine or not
        }

private fun linkStudent(realStudent: Student) {
    val imagineStudent = realStudent.findSameStudent(true)

    if (imagineStudent != null) {
        val parents = Relations.select {
            Relations.child eq imagineStudent.id
        }.map {
            val parentId = it[Relations.parent]
            Parent.findById(parentId) ?: error("Broken link from Parent(id = $parentId)")
        }

        Relations.batchInsert(parents) {
            this[Relations.child] = realStudent.id
            this[Relations.parent] = it.id
        }

        imagineStudent.user.safeDelete()
    }
}

private fun linkParent(parent: Parent) {
    for (child in parent.children) {
        check(child.user.state == IMAGINE) {
            "Unexpected real Student(id = ${child.id})"
        }

        val student = child.findSameStudent(false)
                ?: child.findSameStudent(true)

        if (student != null) {
            Relations.insert {
                it[this.parent] = parent.id
                it[this.child] = student.id
            }

            child.user.safeDelete()
        }
    }
}

val VALIDATION_PROCESSOR = CallbackProcessor("validation") validation@{ coordinator, _, (action, id) ->
    val user = User.findById(id.toInt()) ?: error("User doesn't exist!")

    if (user.valid) {
        coordinator.send(Text["user-was-processed-by-another-coordinator"])
        return@validation
    }

    when (action) {
        "ban" -> {
            user.clear()
            user.state = BANNED
        }

        "dismiss" -> {
            user.clear()

            user.send(Text["registration-dismissed"])

            user.state = READ_NAME
            user.send(Text["register-name"])
        }

        "confirm" -> {
            when {
                user.hasLinked(Student) ->
                    linkStudent(user.linked(Student))

                user.hasLinked(Parent) ->
                    linkParent(user.linked(Parent))
            }
            user.state = COMMAND
            user.valid = true
        }

        else -> error("Action(name = $action) is illegal!")
    }
}