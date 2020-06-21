package minesweeper

import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

fun main() {
    val scanner = Scanner(System.`in`)
    println("How many mines do you want on the field?")
    val totalMines = scanner.nextInt()

    val gameBoard = GameBoard(9, totalMines)

    do {
        gameBoard.printUserView()
        println("Set/unset mines marks or claim a cell as free:")
        val column = scanner.nextInt() - 1
        val row = scanner.nextInt() - 1
        val action = scanner.next()

        if (action == "free") {
            gameBoard.free(row, column)
        } else if (action == "mine") {
            gameBoard.mineMark(row, column)
        }
    } while (gameBoard.gameState == GameState.PROGRESS)
    gameBoard.printUserView()
    if (gameBoard.gameState == GameState.WIN) {
        println("Congratulations! You found all the mines!")
    } else if (gameBoard.gameState == GameState.GAME_OVER) {
        println("You stepped on a mine and failed!")
    }
}

enum class Type {
    EMPTY, BOMB
}

enum class FreeResult {
    OPENED, ALREADY_OPENED, BOMBED
}

enum class GameState {
    PROGRESS, WIN, GAME_OVER
}

class GameBoard {
    val numberOfMines: Int
    var totalCells: Int
    val bombs: List<Cell>
    val board: Array<Array<Cell>>
    var markedCells: Int = 0
    var openedCells: Int = 0
    var gameState: GameState = GameState.PROGRESS

    constructor(size: Int, numberOfMines: Int) {
        this.totalCells = size * size
        this.numberOfMines = numberOfMines
        val board = Array(size) { Array(size) { Cell(Type.EMPTY, false, false, 0, emptyList(), this) } }
        val bombs = ArrayList<Cell>(numberOfMines)

        // set mines
        repeat(numberOfMines) {
            do {
                val i = Random.nextInt(0, size)
                val j = Random.nextInt(0, size)
                val cell = board[i][j]
                if (cell.type == Type.EMPTY) {
                    cell.type = Type.BOMB
                    bombs.add(cell)
                    break
                }
            } while (true)
        }

        // init cells: add neighbors links, count bombs around
        for (i in 0..board.lastIndex) {
            for (j in 0..board[i].lastIndex) {
                val cell = board[i][j]
                val neighbors = mutableListOf<Cell>()
                for (k in Math.max(i - 1, 0)..Math.min(i + 1, board.lastIndex)) {
                    for (l in Math.max(j - 1, 0)..Math.min(j + 1, board[i].lastIndex)) {
                        if (board[k][l] != cell) {
                            neighbors.add(board[k][l])
                        }
                    }
                }
                cell.neighbors = neighbors
                cell.bombsAround = neighbors.stream().filter { it.type == Type.BOMB }.count().toInt()
            }
        }
        this.board = board
        this.bombs = bombs
    }

    fun printUserView() {
        val size = board.size
        print(" |")
        for (i in 1..size) {
            print(i)
        }
        println("|")
        printLineSeparator(size)
        for (i in 0..board.lastIndex) {
            print("${i + 1}|")
            for (cell in board[i]) {
                print(cell.display())
            }
            println("|")
        }
        printLineSeparator(size)
    }

    fun mineMark(row: Int, column: Int) {
        board[row][column].mineMark()
        checkGameState()
    }

    fun free(row: Int, column: Int) {
        val cell = board[row][column]
        val result = cell.free()
        if (result == FreeResult.OPENED && cell.bombsAround == 0) {
            val checked = mutableSetOf(cell)
            val toCheck = LinkedList(cell.neighbors)
            while(toCheck.isNotEmpty()) {
                val neigbor = toCheck.pop()
                if (!checked.contains(neigbor)) {
                    checked.add(neigbor)
                    if (neigbor.free() == FreeResult.OPENED && neigbor.bombsAround == 0) {
                        toCheck.addAll(neigbor.neighbors)
                    }
                }
            }
        }

        if (result == FreeResult.BOMBED) {
            bombs.forEach {it.isOpened = true }
            openedCells += bombs.size
            gameState = GameState.GAME_OVER
        } else {
            checkGameState()
        }
    }

    private fun checkGameState() {
        // check if all empty cells are opened or all bomb are marked
        if ((openedCells == totalCells - numberOfMines) || (markedCells == numberOfMines && checkAllMinesMarked())) {
            gameState = GameState.WIN
        }
    }

    private fun checkAllMinesMarked(): Boolean {
        for (row in board) {
            for (cell in row) {
                if (cell.type == Type.BOMB && !cell.isMarked) {
                    return false
                }
            }
        }
        return true
    }


}

class Cell(var type: Type, var isOpened: Boolean, var isMarked: Boolean, var bombsAround: Int, var neighbors: List<Cell>, val gameboard: GameBoard) {

    fun mineMark() {
        if (isOpened) {
            if (bombsAround == 0) {
                println("Cell already opened!")
            } else {
                println("There is a number here!")
            }
        } else {
            isMarked = !isMarked
            if (isMarked) {
                gameboard.markedCells++
            } else {
                gameboard.markedCells--
            }
        }
    }

    fun free(): FreeResult {
        if (type == Type.BOMB) {
            return FreeResult.BOMBED
        } else if (isOpened) {
            return FreeResult.ALREADY_OPENED
        } else {
            isOpened = true
            gameboard.openedCells++
            if (isMarked) {
                isMarked = false
                gameboard.markedCells--
            }
            return FreeResult.OPENED
        }
    }

    fun display(): Char {
        return if (isMarked) {
            '*'
        } else if (!isOpened) {
            '.'
        } else {
            if (type == Type.EMPTY) {
                if (bombsAround == 0) {
                    return '/'
                } else {
                    return bombsAround.toString().first()
                }
            } else if (type == Type.BOMB) {
                return 'X'
            } else{
                throw IllegalStateException("Can't display cell $this")
            }
        }
    }


}

fun createBoard(size: Int, numberOfMines: Int): Array<CharArray> {
    val board = Array(size) { CharArray(size) { '.' } }
    repeat(numberOfMines) {
        do {
            val i = Random.nextInt(0, size)
            val j = Random.nextInt(0, size)
            val cell = board[i][j]
            if (cell == '.') {
                board[i][j] = 'X'
            }
        } while (cell == 'X')
    }
    return board
}

private fun addNumbers(board: Array<CharArray>) {
    for (i in 0..board.lastIndex) {
        for (j in 0..board[i].lastIndex) {
            if (board[i][j] != 'X') {
                var counter = 0
                for (k in Math.max(i - 1, 0)..Math.min(i + 1, board.lastIndex)) {
                    for (l in Math.max(j - 1, 0)..Math.min(j + 1, board[i].lastIndex)) {
                        if (board[k][l] == 'X') {
                            counter++
                        }
                    }
                }
                if (counter > 0) {
                    board[i][j] = counter.toString().first()
                }
            }
        }
    }
}

private fun copyWithHidedMines(board: Array<CharArray>): Array<CharArray> {
    val copied = Array(board.size) { CharArray(board[0].size) { '.' } }
    for (i in 0..board.lastIndex) {
        for (j in 0..board[i].lastIndex) {
            val value = board[i][j]
            copied[i][j] = if (value == 'X') '.' else value
        }
    }
    return copied
}

fun checkCorrect(board: Array<CharArray>, userBoard: Array<CharArray>): Boolean {
    for (i in 0..board.lastIndex) {
        for (j in 0..board[i].lastIndex) {
            if (board[i][j] == 'X' && userBoard[i][j] != '*') {
                return false
            }
        }
    }
    return true
}

fun drawBoard(board: Array<CharArray>) {
    val size = board.size
    print(" |")
    for (i in 1..size) {
        print(i)
    }
    println("|")
    printLineSeparator(size)
    for (i in 0..board.lastIndex) {
        print("${i + 1}|")
        for (cell in board[i]) {
            print("$cell")
        }
        println("|")
    }
    printLineSeparator(size)
}

fun printLineSeparator(size: Int) {
    print("-|")
    repeat(size) {
        print("-")
    }
    println("|")
}
