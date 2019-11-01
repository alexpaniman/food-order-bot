package org.order.data.entities

/**
 * This enumeration describes all users states.
 *
 * It's event driven bot style consequence.
 */

enum class State {
    NEW, // State which gives to new users. User has this state during name and phone registration

    IMAGINE, // State for children created by their parents. This users doesn't have linked chat

    COMMAND, // Waiting for regular command. It's normal state for most system users

    READ_NAME,  // Waiting for user name  when user isn't valid
    READ_PHONE, // Waiting for user phone when user isn't valid

    READ_STATE, // Waiting for user state after user registration

    READ_GRADE, // Waiting for grade input (for not valid students only)

    CHOOSES_ROLES, // Basic user information is registered but user doesn't yet select all his roles

    READ_CHILD_NAME,  // Waiting for user name  provider by parent
    READ_CHILD_LINK,  // Waiting for student string in "name, grade" format for linking
    READ_CHILD_GRADE, // Waiting for user grade provided by parent
    CONFIRM_CHILD_ADDING, // Waiting for confirmation or dismissing add yet child suggestion

    VALIDATION; // User registration is done and user validation request was sent to the admin or to corresponding coordinators
}