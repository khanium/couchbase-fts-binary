$(document).ready(function() {
    $(document).on('submit', '#search_form', function() {
        // do your things
        try {
            submit_search();
        } catch (e) {
          console.error(e);
        }
        return false;
    });
});


async function submit_search() {
    console.log("submit search...");
    let input = document.getElementById( "inputSearch" ).value;
    if( input && input !== "") {
        console.log("input searh: ", input);
        // Bind the FormData object and the form element
        let response = await fetch('/binaries/searching', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json;charset=utf-8'
            },
            body: input
        });

        let data = await response.json();
        /* sample response data:
        let data = { total: 3,
                     hits: [{ thumbnail: "pdf.jpg", author: "j.molina", highlights: "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Voluptatem, exercitationem, suscipit, distinctio, qui sapiente aspernatur molestiae non corporis magni sit sequi iusto debitis delectus doloremque.", registeredAt: "2019-01-20 20:00", title: "searchable:sample1.pdf", reference:"sample1.pdf", tags:"pdf, printer"},
                            { thumbnail: "pdf.jpg", author: "j.molina", highlights: "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Voluptatem, exercitationem, suscipit, distinctio, qui sapiente aspernatur molestiae non corporis magni sit sequi iusto debitis delectus doloremque.", registeredAt: "2019-12-30 10:45", title: "searchable:sample2.pdf", reference:"sample2.pdf", tags:"pdf, printer"},
                            { thumbnail: "pdf.jpg", author: "j.molina", highlights: "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Voluptatem, exercitationem, suscipit, distinctio, qui sapiente aspernatur molestiae non corporis magni sit sequi iusto debitis delectus doloremque.", registeredAt: "2020-01-02 22:00", title: "searchable:sample3.pdf", reference:"sample3.pdf", tags:"pdf, printer"}] };
        */
        console.log('-- found: ', JSON.stringify(data));
        display(data);
    }
    return false;
}

function display(data) {
    let searchTerms = document.getElementById("inputSearch").value;
    let total = data.total;
    let results = data.hits;
    let content = document.getElementById("results");
    let grid = document.getElementById("results-grid");
    let header = document.getElementById("header-display");

    display_header(header, searchTerms, total);
    display_grid(grid, results);
    content.hidden=false; // show results

}

function display_header(header, searchTerms, total) {
    header.innerHTML = "<strong class=\"text-danger\">"+total+"</strong> results were found for the search for <strong class=\"text-danger\">"+searchTerms+"</strong>";
}

function display_grid(grid, data) {
    // clear previous result list
    grid.innerHTML = "";
    // adding found documents rows
    data.forEach(doc => addArticule(grid, doc));
}



function addArticule(grid, item) {
   // console.log('item: ',item);
    const id = item.id;
    const highlights = item.highlights;
    const title = item.title || item.id;
    const calendar = new Date(item.registeredAt).toDateString();
    const time = new Date(item.registeredAt).toLocaleTimeString();
    const tags = item.tags|| '--';
    const author = item.author || 'unknown';
    const image = "images/"+(item.thumbnail || 'pdf.jpg');
    const reference = "files/"+item.reference;
    const htmlToElement = html => {
        const placeholder = document.createElement('div');
        placeholder.innerHTML = html;
        return placeholder.children.length ? placeholder.firstElementChild : undefined;
    };
    const fragment = document.createDocumentFragment();
    const node = htmlToElement(
    " <article class=\"search-result row\">\n" +
        "                    <div class=\"col-xs-12 col-sm-12 col-md-2\">\n" +
        "                        <a href=\"details?id="+id+"\" title=\""+reference+"\" class=\"thumbnail\"><img class='img-fluid rounded float-left' src=\""+image+"\" alt=\""+reference+"\" /></a>\n" +
        "                    </div>\n" +
        "                    <div class=\"col-xs-12 col-sm-12 col-md-1\">\n" +
        "                        <ul class=\"meta-search\">\n" +
        "                            <li><i class=\"fa fa-user\"></i> <span>"+author+"</span></li>\n" +
        "                            <li><i class=\"fa fa-calendar\"></i> <span>"+calendar+"</span></li>\n" +
        "                            <li><i class=\"fas fa-clock\"></i> <span>"+time+"</span></li>\n" +
        "                            <li><i class=\"fa fa-tags\"></i> <span>"+tags+"</span></li>\n" +
        "                        </ul>\n" +
        "                    </div>\n" +
        "                    <div class=\"col-xs-12 col-sm-12 col-md-9 excerpet\">\n" +
        "                        <h3><a href=\"details?id="+id+"\" title=\"\">"+title+"</a></h3>\n" +
        "                        <p>"+highlights+"</p>\n" +
        "                        <span class=\"plus\"><a href=\""+reference+"\" title=\""+reference+"\"><i class=\"fas fa-download\"></i></a></span>\n" +
        "                    </div>\n" +
        "                    <span class=\"clearfix borda\"></span>\n" +
        "                </article>");
    fragment.appendChild( node);
    grid.appendChild(fragment);

}