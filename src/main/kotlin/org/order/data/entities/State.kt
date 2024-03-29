package org.order.data.entities

/**
 * This enumeration describes all users states.
 *
 * It's event driven bot style consequence.
 */

enum class State {
    NEW, // State which gives to new users. User has this state during name and phone registration
    BANNED, // User was banned

    IMAGINE, // State for children created by their parents. This users doesn't have linked chat

    COMMAND, // Waiting for regular command. It's normal state for most system users

    READ_NAME,  // Waiting for user name  when user isn't valid
    READ_PHONE, // Waiting for user phone when user isn't valid

    READ_ROLE, // Waiting for user state after user registration

    READ_GRADE, // Waiting for grade input (for not valid students only)

    CHOOSE_ROLE, // Basic user information is registered but user doesn't yet select all his roles

    READ_CHILD_NAME,  // Waiting for user name  provider by parent
    READ_CHILD_GRADE, // Waiting for user grade provided by parent
    CONFIRM_CHILD_ADDING, // Waiting for confirmation or dismissing add yet child suggestion

    REGISTRATION_FINISHED, // User registration is done and user validation request was sent to the admin or to corresponding coordinators
    CONFIRM_REGISTRATION,
    VALIDATION,

    READ_SEARCH_STRING, // Used by user searcher module

    READ_CLIENT_TO_REPLENISH_ACCOUNT,
    CHOOSE_CLIENT_TO_REPLENISH_ACCOUNT,
    READ_PAYMENT_AMOUNT_TO_REPLENISH_ACCOUNT,

    READ_PAYMENT_AMOUNT,
    ASK_WHICH_CLIENT_TO_PAY_FOR,

    READ_DATE_FOR_PDF_POLLS_TOTAL,

    READ_COMMENT_TO_ORDER,

    READ_MAILING_TYPE,

    SEND_MESSAGE_WITH_KEYBOARD_TO_ALL_USERS,
    SEND_MESSAGE_TO_ALL_USERS,
    SEND_MESSAGE_TO_ALL_CLIENTS,
    SUGGEST_REGISTRATION_TO_ALL_NEW_USERS,

    READ_REFUND_COMMENT,
    READ_REFUND_AMOUNT;
}