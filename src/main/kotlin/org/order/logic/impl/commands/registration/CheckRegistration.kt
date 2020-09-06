package org.order.logic.impl.commands.registration

import org.jetbrains.exposed.sql.insert
import org.order.bot.send.*
import org.order.data.entities.*
import org.order.data.entities.State.*
import org.order.data.tables.Relations
import org.order.data.tables.Students
import org.order.logic.commands.TriggerCommand
import org.order.logic.commands.processors.CallbackProcessor
import org.order.logic.commands.triggers.CommandTrigger
import org.order.logic.commands.triggers.StateTrigger
import org.order.logic.corpus.Text
import org.order.logic.impl.utils.appendMainKeyboard
import org.order.logic.impl.utils.isRegistered
import org.telegram.telegrambots.meta.api.objects.Message

private fun User.descriptionForValidation() = buildDescription(Student, Teacher, Parent, Producer)
        .replace('─', '-')
        .replace('│', '|')
        .replace("[┼┐┌└┘┤├]".toRegex(), " ")
        .lines().joinToString("\n") { "`$it`" }

val CHECK_REGISTRATION = TriggerCommand(trigger = StateTrigger(REGISTRATION_FINISHED)) { user, _ ->
    user.send(Text.get("registration-summary") {
        it["description"] = user.descriptionForValidation()
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

                else -> error("This user has no appropriate linked role!")
            }

            val description = user.descriptionForValidation()

            val message = Text.get("validation") {
                it["description"] = description
            }

            for (target in targets.distinct())
                target.send(message) {
                    inline {
                        button(Text["validation-ban"], "validation:ban:${user.id.value}")
                        button(Text["validation-confirm"], "validation:confirm:${user.id.value}")
                        button(Text["validation-dismiss"], "validation:dismiss:${user.id.value}")
                    }
                }

            user.send(Text.get("registration-confirmed") {
                it["description"] = description
            }) { removeReply() }

            user.state = VALIDATION
        }

        Text["registration-dismiss-button"] -> {
            user.clear()

            user.send(Text["registration-dismissed"]) {
                removeReply()
            }

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
                    it.user.valid && // User has passed validation
                    it != this && // The student isn't this student
                    (it.user.state != IMAGINE) xor imagine // According to imagine variable student imagine or not
        }

private fun linkStudent(realStudent: Student): User {
    val imagineStudent = realStudent.findSameStudent(true)

    return if (imagineStudent != null) {
        imagineStudent.user.apply {
            chat = realStudent.user.chat
            phone = realStudent.user.phone
            state = COMMAND
        }

        realStudent.user.safeDelete()

        imagineStudent.user
    } else realStudent.user.apply {
        state = COMMAND
        valid = true
    }
}

private fun linkParent(parent: Parent): User {
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
        } else
            child.user.valid = true
    }

    return parent.user.apply {
        state = COMMAND
        valid = true
    }
}

val RESEND_BUTTONS = TriggerCommand(CommandTrigger(Text["resend-buttons-command"])) { user, _ ->
    user.send(Text["resend-buttons-message"]) {
        appendMainKeyboard(user)
    }
}

val VALIDATION_PROCESSOR = CallbackProcessor("validation") { _, src, (action, id) ->
    performValidation(src, action, id.toInt())
}

fun SenderContext.performValidation(src: Message?, action: String, id: Int) {
    val user = User.findById(id)

    if (user == null) {
        src?.edit(Text["coordinator-notification:looks-like-another-coordinator-have-processed-it"])
        return
    }

    val description = user.descriptionForValidation()

    if (user.valid || user.state == BANNED || user.isRegistered) {
        src?.edit(Text.get("coordinator-notification:user-was-processed-by-another-coordinator") {
            it["description"] = description
        })
        return
    }

    when (action) {
        "ban" -> {
            user.clear()
            user.state = BANNED

            user.send(Text["validation:user-is-banned"])

            src?.edit(Text.get("coordinator-notification:user-is-banned") {
                it["description"] = description
            })
        }

        "dismiss" -> {
            user.clear()

            user.send(Text["validation:user-is-dismissed"])

            user.state = READ_NAME
            user.send(Text["register-name"])

            src?.edit(Text.get("coordinator-notification:user-is-dismissed") {
                it["description"] = description
            })
        }

        "confirm" -> {
            val confirmedUser = when {
                user.hasLinked(Student) ->
                    linkStudent(user.linked(Student))

                user.hasLinked(Parent) ->
                    linkParent(user.linked(Parent))

                user.hasLinked(Teacher) || user.hasLinked(Producer) -> user.apply {
                    state = COMMAND
                    valid = true
                }

                else -> error("Illegal kind of users!")
            }

            confirmedUser.send(Text["validation:user-is-confirmed"]) { appendMainKeyboard(confirmedUser) }

            src?.edit(Text.get("coordinator-notification:user-is-confirmed") {
                it["description"] = description
            })
        }

        else -> error("Action(name = $action) is illegal!")
    }
}