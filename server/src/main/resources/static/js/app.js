let stompClient = null;

function setConnected(connected) {
    if (connected) {
        $("#dot").css("color", "#49660e");
    } else {
        $("#dot").css("color", "#bbb");
    }
}

function connect() {
    const socket = new SockJS('/metis-websocket');
    stompClient = Stomp.over(socket);
    stompClient.debug = () => {
    };
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
    $("#pool-pending").text(message.pending);
}

function translate(message) {
    switch (message) {
        case 'check-started':
            return 'Ο έλεγχος ξεκίνησε...';
        case 'files-cleaned':
            return 'Τα αποτελέσματα εκκαθαρίστηκαν.';
        case 'password-changed':
            break
        case 'password-not-changed':
            break
    }
}
