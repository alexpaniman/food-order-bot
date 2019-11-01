package org.order.logic.impl.commands.registration.parent

import org.jetbrains.exposed.sql.insert
import org.order.bot.send.Sender
import org.order.bot.send.button
import org.order.bot.send.reply
import org.order.bot.send.show
import org.order.data.entities.Parent

import org.order.data.entities.Student
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.data.tables.Parents
import org.order.data.tables.Relations

import org.order.logic.commands.readers.TextReader
import org.order.logic.corpus.Text

private const val NAME_VALIDATOR = "$[А-ЯЁ][а-яё]+(-[А-ЯЁ][а-яё]+)? [А-ЯЁ][а-яё]+\$"

fun Sender.readChildName(user: User) = user.run {
    send(Text["register-child-name"])

    state = State.READ_CHILD_GRADE
}

val READ_CHILD_NAME = TextReader(State.READ_CHILD_NAME) reader@ { user, name ->
    val isInputValid = name matches NAME_VALIDATOR.toRegex() // Validate received from user text

    if (isInputValid) {
        val userAsParent = Parent
                .find { Parents.user eq user.id } // Find linked to this user parent
                .single() // Only one parent can be linked to this user

        val currentStudent = userAsParent
                .children
                .singleOrNull { it.grade == null } // If staged user exists fetch it

                ?: Student.new { // Else create new student

                    this.user = User.new {
                        this.state = State.IMAGINE // Users with "imagine" state users hasn't telegram account
                        this.valid = false

                        this.name = name
                    }
                }

        val otherStudents = Student.all() // FIXME Move filter operation to database
                .filter {
                           it.user.name  ==          name // Student name is the same as received name
                        && it     .grade !=          null // Student grade isn't empty (user completed registration)
                }
                .map { "$name, ${it.grade}" } // Create marks for users

        if (otherStudents.isNotEmpty()) {
            // If students with the same name exists we suggest user to link account with them
            user.send(Text["link-child"]) {
                reply {
                    show(otherStudents, 1)

                    button(Text["link-new-user"]) // Suggest anyway link new student
                }
            }

            user.state = State.READ_CHILD_LINK
            return@reader true // Break the flow or wait for a answer
        }
        user.state = State.CHOOSES_ROLES
    } else user.send(Text["wrong-name"]) // If input name is invalid we ask user to retype name
    return@reader false
}

val READ_CHILD_LINK = TextReader(State.READ_CHILD_LINK) reader@{ user, marker ->
    if (marker == Text["link-new-user"])
        return@reader false // Move in flow to reading grade
    val args = marker.split(", ")

    if (args.size != 2) { // If label contains more then one ", " then label is invalid
        user.send(Text["wrong-link"])
        return@reader true // Break the flow
    }

    val (name, grade) = args
    val receivedStudent = Student.all()
            .singleOrNull { it.grade?.name == grade && it.user.name == name }
    // FIXME It's slow because it loads all students to memory. Move filter operation from memory to database

    if (receivedStudent == null) {
        user.send(Text["wrong-link"])
        return@reader true // Break the flow
    }

    val userAsParent = Parent
            .find { Parents.user eq user.id } // Find linked to this user parent
            .single() // Only one parent can be linked to this user

    val currentStudent = userAsParent
            .children
            .single { it.grade == null } // This user was created on previous step [see. READ_CHILD_NAME above]
    // FIXME Move filter operation to database

    Relations.insert {
        it[parent] = userAsParent   .id
        it[ child] = receivedStudent.id
    }

    currentStudent.delete()

    user.state = State.CHOOSES_ROLES

    return@reader false
}