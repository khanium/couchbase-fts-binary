window.onload = (event) => {
    try {
        let item_id = document.location.search.replace(/^.*?\=/, '');
        load_details(item_id);
    } catch (e) {
        //TODO handle errors
        console.error(e);
        alert(e);
    }
};


async function load_details(item_id){
    console.log("loading details from...",item_id);
    let response = await fetch('/binary/'+item_id);
    let item = await response.json();
    console.log("item to load: ", item);
    display(item);
}

function display(item) {
    console.log("to display here...")
}