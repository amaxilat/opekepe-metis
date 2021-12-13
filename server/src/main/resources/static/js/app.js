var stompClient = null;

function setConnected(connected) {
    if (connected) {
        $("#dot").css("background-color", "green");
    } else {
        $("#dot").css("background-color", "#bbb");
    }
}

function connect() {
    var socket = new SockJS('/metis-websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        stompClient.subscribe('/topic/pool', function (greeting) {
            updatePoolInfo(JSON.parse(greeting.body));
        });
    }, function () {
        disconnect();
        setTimeout(function () {
            connect();
        }, 3000);
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
}

function updatePoolInfo(message) {
    $("#pool-active").text(message.active);
    $("#pool-max").text(message.max);
    $("#pool-pending").text(message.pending);
}
