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

function connect_cloud_detection() {
    $.get("/v1/api/cloud", function (data) {
        $("#dot-cloud").css("color", "#49660e");
        $("#cloud-model").text(data['model']);
    }).fail(function () {
        $("#dot-cloud").css("color", "#bbb");
        $("#cloud-model").text('');
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
