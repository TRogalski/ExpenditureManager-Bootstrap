document.addEventListener('DOMContentLoaded', function () {

    $('#date_picker').datepicker({
        format: 'yyyy-mm-dd',
    }).datepicker("setDate", 'now');


    $('#date_picker').on('changeDate', function () {
        getExpendituresAssignedToDate($('#date_picker').datepicker('getFormattedDate'));
    });


})

//Show elements based on element clicked

function getExpendituresAssignedToDate(date) {
    fetch("http://localhost:8084/expenditure/date/" + date).then(function (response) {
        return response.json();
    }).then(function (dateExpendituresJson) {
        // console.log(JSON.stringify(dateExpendituresJson));
        appendReceivedElements(dateExpendituresJson)
    });

    removeEnlistedElements()
}

function removeEnlistedElements() {
    var toDelete = document.getElementById("expenditure_records");
    while (toDelete.hasChildNodes()) {
        toDelete.removeChild(toDelete.lastChild);
    }
}

function appendReceivedElements(dateExpendituresJson) {

    for (var i = 0; i < dateExpendituresJson.length; i++) {
        var tableRow = $(`<tr>
                            <td>${dateExpendituresJson[i].category.name}</td>
                            <td>${dateExpendituresJson[i].name}</td>
                            <td>${dateExpendituresJson[i].amount}</td>
                            <td>${dateExpendituresJson[i].description}</td>
                       </tr>`);
        $("#expenditure_records").append(tableRow)
    }
}