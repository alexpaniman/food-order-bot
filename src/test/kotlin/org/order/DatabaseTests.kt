package org.order
/*

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.order.data.entities.*
import org.order.data.tables.*
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.JVM)
class DatabaseTests {
    private fun createGrade(name: String) = Grade.new { this.name = name }
    private fun createRight(name: String) = Right.new { this.name = name }
    private fun createMenu(name: String, cost: Int) = Menu.new {
        this.name = name
        this.cost = cost
        this.active = true
    }
    private fun createIngredient(name: String, menu: Menu) = Ingredient.new {
        this.name = name
        this.menu = menu
    }
    private fun createOrder(user: User, menu: Menu) = Order.new {
        this.menu = menu
        this.user = user
    }
    private fun createPayment(date: DateTime, order: Order) = Payment.new {
        this.date = date
        this.order = order
    }

    private fun createGrades() = (10..20).map { createGrade("grade-${(0..999).random()}") }
    private fun createRights() = (2..7).map { createRight("right-${(0..999).random()}") }
    private fun createMenus() = (23..50).map { createMenu("menu-${(0..999).random()}", (0..999).random()) }
    private fun createUsers(grades: List<Grade>, rights: List<Right>) = (150..300).map {
        User.new {
            name = "user-${(0..999).random()}"
            phone = "+" + (0L..999999999999L).random().toString().padEnd(12, '0')
            grade = grades.random()
            right = rights.random()
            telegramId = (0..Int.MAX_VALUE).random()

            state = ""
        }
    }
    private fun createIngredients(menus: List<Menu>) = (150..300).map { createIngredient("ingredient-${(0..999).random()}", menus.random()) }
    private fun createOrders(users: List<User>, menus: List<Menu>) = (80..200).map { createOrder(users.random(), menus.random()) }
    private fun createPayments(orders: List<Order>) = (30..120).map { createPayment(DateTime.now(), orders.random()) }

    @Test fun createData() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(Grades, Ingredients, Menus, Orders, Payments, Rights, Users)

            val grades = createGrades()
            val rights = createRights()
            val menus = createMenus()
            val users = createUsers(grades, rights)
            createIngredients(menus)
            val orders = createOrders(users, menus)
            createPayments(orders)
        }
    }

    private fun assertTrueInTransaction(statement: () -> Boolean) = assertTrue {
        transaction {
            statement()
        }
    }

    @Test fun testGradeUser() = assertTrueInTransaction {
        val user = User.all().toList().random()
        user.grade!!.users.contains(user)
    }

    @Test fun testRightUser() = assertTrueInTransaction {
        val user = User.all().toList().random()
        user.right.users.contains(user)
    }

    @Test fun testOrderUser() = assertTrueInTransaction {
        val user = User.all().toList().random()
        val orders = user.orders
        if (!orders.empty()) {
            val order = orders.toList().random()
            order.user == user
        } else true
    }

    @Test fun testIngredientMenu() = assertTrueInTransaction {
        val menu = Menu.all().toList().random()
        val ingredients = menu.ingredients
        if (!ingredients.empty()) {
            ingredients.toList().random().menu == menu
        } else true
    }

    @Test fun testUsersCount() = assertTrueInTransaction {
        User.count() in 150..300
    }

    @Test fun testGradesCount() = assertTrueInTransaction {
        Grade.count() in 10..20
    }

    @Test fun testRightsCount() = assertTrueInTransaction {
        Right.count() in 2..7
    }

    @Test fun testMenusCount() = assertTrueInTransaction {
        Menu.count() in 23..50
    }

    @Test fun testIngredientsCount() = assertTrueInTransaction {
        Ingredient.count() in 150..300
    }

    @Test fun testOrdersCount() = assertTrueInTransaction {
        Order.count() in 80..200
    }

    @Test fun testPaymentsCount() = assertTrueInTransaction {
        Payment.count() in 30..120
    }
}*/