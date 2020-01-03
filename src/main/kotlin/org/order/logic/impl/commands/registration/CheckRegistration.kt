package org.order.logic.impl.commands.registration

import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.order.bot.send.button
import org.order.bot.send.inline
import org.order.data.entities.*
import org.order.data.entities.State.*
import org.order.data.tables.Relations
import org.order.data.tables.Students
import org.order.logic.commands.callback.CallbackProcessor
import org.order.logic.commands.triggers.StateTrigger
import org.order.logic.commands.TriggerCommand
import org.order.logic.corpus.Text

val CHECK_REGISTRATION = TriggerCommand(trigger = StateTrigger(VALIDATION)) { user, _ ->
    user.send(Text.get("registration-summary") {
        it["description"] = user.buildDescription(Student, Teacher, Parent, Producer)
    }) {
        inline {
            button(Text["registration-confirm-button"], "registration:confirm")
            button(Text["registration-dismiss-button"], "registration:dismiss")
        }
    }
}

val REGISTRATION_PROCESSOR = CallbackProcessor("registration") { user, src, (action) ->
    when (action) {
        "confirm" -> {
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

            src.edit(Text.get("registration-confirmed") {
                it["description"] = description
            })
        }

        "dismiss" -> {
            user.clear()

            user.send(Text["registration-dismissed"])

            user.state = READ_NAME
            user.send(Text["register-name"])
        }

        else -> error("illegal action: $action")
    }
}

private fun Student.findSameStudent(imagine: Boolean): Student? = Student.find { Students.grade eq grade!!.id }
        .singleOrNull {
            it.user.name == user.name && // Student has same name
                    it != this && // Student isn't this student
                    (it.user.state == IMAGINE) xor imagine
        }

private fun Student.linkStudent() {
    val sameStudent = findSameStudent(true)

    if (sameStudent != null) {
        val sameUser = sameStudent.user

        sameUser.chat  = user.chat // Linking this chat to same user
        sameUser.state = COMMAND

        user.delete() // Remove old user
    }
}

private fun Parent.linkParent() {
    for (child in children) {
        val same = child.findSameStudent(false)

        if (same != null) {
            Relations.insert {
                // Link with already exists user
                it[this.parent] = id
                it[this.child] = same.id
            }

            Relations.deleteWhere {
                Relations.child eq child.id // Remove relation with this child
            }

            child.user.delete() // Delete old child
            child.delete()
        }
    }
}

val VALIDATION_PROCESSOR = CallbackProcessor("validation") { _, _, (action, id) ->
    val user = User.findById(id.toInt()) ?: error("user not found") // TODO: user could be removed by another coordinator

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
                user.hasLinked(Student) -> user.linked(Student).linkStudent()

                user.hasLinked(Parent) -> {
                    user.linked(Parent).linkParent()
                    user.state = COMMAND
                }

                else -> user.state = COMMAND
            }
        }

        else -> error("illegal action: $action")
    }
}