import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import x11.Atom
import x11.AtomVar
import x11.ClientMessage
import x11.Display
import x11.SubstructureNotifyMask
import x11.SubstructureRedirectMask
import x11.Window
import x11.XA_WINDOW
import x11.XCloseDisplay
import x11.XDefaultRootWindow
import x11.XDefaultScreen
import x11.XEvent
import x11.XFetchName
import x11.XGetWindowProperty
import x11.XInternAtom
import x11.XOpenDisplay
import x11.XRootWindow
import x11.XSendEvent

@OptIn(ExperimentalForeignApi::class)
private fun sendMessage(
    display: CPointer<Display>, dstWindow: Window, message: String,
    data0: Long = 0, data1: Long = 0, data2: Long = 0, data3: Long = 0, data4: Long = 0
): Int {
    memScoped {
        val messageType = XInternAtom(display, message, 0)
        val event = alloc<XEvent> {
            xclient.apply {
                type = ClientMessage
                serial = 0u
                send_event = 1
                message_type = messageType
                window = dstWindow
                format = 32
                data.l[0] = data0
                data.l[1] = data1
                data.l[2] = data2
                data.l[3] = data3
                data.l[4] = data4
            }
        }

        return XSendEvent(display, XDefaultRootWindow(display), 0, SubstructureRedirectMask or SubstructureNotifyMask, event.ptr)
    }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
private fun <T> getWindowProperty(
    display: CPointer<Display>, dstWindow: Window, message: String, reqType: Atom,
    propertyMapper: (nItems: ULongVar, retProp: CPointerVar<UByteVar>) -> T
): T {
    return memScoped {
        val msgAtom = XInternAtom(display, message, 0)
        val win = XDefaultRootWindow(display)

        val type = alloc<AtomVar>()
        val format = alloc<IntVar>()
        val nItems = alloc<ULongVar>()
        val bytesAfter = alloc<ULongVar>()
        val retProp = alloc<CPointerVar<UByteVar>>()
        val result = XGetWindowProperty(
            display, win, msgAtom, 0, 1024, 0, reqType,
            type.ptr, format.ptr, nItems.ptr, bytesAfter.ptr, retProp.ptr
        )

        println("win: $dstWindow reqType: $reqType type: ${type.value}")

        if (result != 0) throw RuntimeException("XGetWindowProperty ${dstWindow.toHexString()} message: $message reqType: $reqType Result: $result")

        propertyMapper.invoke(nItems, retProp)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun getClients(display: CPointer<Display>): List<Window> {
    val screenNumber = XDefaultScreen(display)
    val desktop = XRootWindow(display, screenNumber)
    val msg = "_NET_CLIENT_LIST"

    return getWindowProperty(display, desktop, msg, XA_WINDOW) { nItems, retProp ->
        (0..<nItems.value.toInt()).map { i ->
            val retBytes = UByteArray(8) { retProp.value?.get((i * 8) + it)!! }
            retBytes.getULongBe()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
fun main() {
    val display = XOpenDisplay(null) ?: error("Can't open display")
    val windows = getClients(display)

    for (window in windows) {
        val windowName = memScoped {
            alloc<CPointerVar<ByteVar>> {
                XFetchName(display, window, ptr)
            }.value?.toKString() ?: "Unknown"
        }

        println("Window Name: $windowName")

        if (windowName.contains("Workspaces")) {
            sendMessage(display, window, "_NET_ACTIVE_WINDOW")
        }
    }

    XCloseDisplay(display)
}


fun UByteArray.getULongBe() =
    ((this[7].toULong() and 0xFFu) shl 56) or
            ((this[6].toULong() and 0xFFu) shl 48) or
            ((this[5].toULong() and 0xFFu) shl 40) or
            ((this[4].toULong() and 0xFFu) shl 32) or
            ((this[3].toULong() and 0xFFu) shl 24) or
            ((this[2].toULong() and 0xFFu) shl 16) or
            ((this[1].toULong() and 0xFFu) shl 8) or
            (this[0].toULong() and 0xFFu)

// Not used but handy.
@Suppress("UNUSED")
fun UByteArray.getULongLe() =
    ((this[0].toULong() and 0xFFu) shl 56) or
            ((this[1].toULong() and 0xFFu) shl 48) or
            ((this[2].toULong() and 0xFFu) shl 40) or
            ((this[3].toULong() and 0xFFu) shl 32) or
            ((this[4].toULong() and 0xFFu) shl 24) or
            ((this[5].toULong() and 0xFFu) shl 16) or
            ((this[6].toULong() and 0xFFu) shl 8) or
            (this[7].toULong() and 0xFFu)
