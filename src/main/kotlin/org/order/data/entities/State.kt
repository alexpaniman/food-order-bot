package org.order.data.entities

enum class State {
    NEW, // State which gives to new users

    COMMAND, // Waiting for regular command

    READ_NAME,  // Waiting for user name  when user isn't valid
    READ_PHONE, // Waiting for user phone when user isn't valid

    READ_STATE, // Waiting for user state after user registration

    READ_GRADE, // Waiting for grade input (for not valid 'client' only)

    READ_CHILD_NAME,  // Waiting for user name  provider by parent
    READ_CHILD_GRADE; // Waiting for user grade provided by parent
}